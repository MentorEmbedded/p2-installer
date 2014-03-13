/*
 *                  Copyright (c) 2013 Mentor Graphics
 *
 * PROPRIETARY RIGHTS of Mentor Graphics are involved in the
 * subject matter of this material.  All manufacturing, reproduction,
 * use, and sales rights pertaining to this subject matter are governed
 * by the license agreement.  The recipient of this software implicitly
 * accepts the terms of the license.
 *
 *****************************************************************************/
#ifdef _WIN32
#include <windows.h>
#include <shlobj.h>
#include <objbase.h>
#include <objidl.h>
#else
#include <unistd.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/types.h>
#include <signal.h>
#include <dirent.h>
#include <sys/stat.h>
#include <string.h>

#include <fcntl.h>
#include <errno.h>

#ifdef _WIN32
#define DIRSEPSTR "\\"
#define PATHSEPSTR ";"
#else
#define DIRSEPSTR "/"
#define PATHSEPSTR ":"
#endif

/* Flag for accessing 64-bit registry node */
#define KEY_WOW64_64KEY 0x0100

/* Log file */
FILE *logFile = NULL;
/* File command line options */
char ** fargv = NULL;
/* Number of file command line options */
int fargc;

/**
 * Prints help
 *
 * @param stream Stream to print help
 * @return None
 */
static void
usage (FILE *stream)
{
  fprintf (stream,
    "\n"
    "Usage: instmon <option(s)>\n"
    " Mentor Embedded Installer Utility\n"
    " Options are:\n"
    "  -h --help                    Print this message, and exit.\n"
    "  -getOsProperty <property>    Returns the value of an operating specific\n"
    "                               property.  Supported properties are:\n"
    "                                 WIN_MAJOR_VERSION - Windows major version\n"
    "                                 WIN_MINOR_VERSION - Windows minor version\n"
    "  -runAdmin \"<path>,<args>\"\n"
    "                               Runs a program with elevated rights.\n"
    "                               <path> The path to the executable to run.\n"
    "                               <args> The arguments to pass to the\n"
    "                                 executable.  Use ' to quote arguments.\n"
    "  -removeDir <paths>           Comma separated list of directories to\n"
    "                               remove.\n"
    "  -removeEmptyDir <paths>      Comma separated list of directories to\n"
    "                               remove only if they are empty.\n"
    "  -regSetValue \"<key>,<name>,<value>,<type>\"\n"
    "                               Sets a Windows registry value, where\n"
    "                               key is the fully qualified key path\n"
    "                               name is the name of the registry key\n"
    "                               value is the value for the registry entry\n"
    "                               type is the type of registry value\n"
    "                               (string,dword)\n"
    "  -regGetValue \"<key>,<name>\"\n"
    "                               Gets a Windows registry value, where\n"
    "                               key is the fully qualified key path\n"
    "                               name is the name of the registry key\n"
    "  -regDeleteValue \"<key>,<name>\"\n"
    "                               Deletes a named registry value.\n"
    "  -regDeleteKey \"<key>\"        Deletes a registry key.\n"
    "  -getSpecialFolder \"<clsid>\"  Prints the path to a special folder.\n"
    "                               <clsid> The folder CSIDL (see Windows API).\n"
    "  -createShortcut \"<path>,<linkName>,<targetFile>,<args>,\n"
    "                   <description>,<showMode>,<workingDirectory>,\n"
    "                   <iconFile>,<iconIndex>\"\n"
    "                               Creates a short-cut.\n"
    "                               <path> The path for the short-cut folder.\n"
    "                               <targetFile> Full path to the short-cut  \n"
    "                               target file.                             \n"
    "                               <arg> The arguments for the short-cut.   \n"
    "                               <linkName> Name for the short-cut.       \n"
    "                               <description> Optional description.      \n"
    "                               <showMode> Optional show mode.           \n"
    "                               <workingDirectory> Working directory for \n"
    "                               the short-cut.                           \n"
    "                               <iconFile> Full path to the icon file.   \n"
    "                               <iconIndex> Index of icon in icon file.  \n"
    "  -pid <pid>                   PID of process to wait for termination\n"
    "                               before removing directories.\n"
    "  -wait <seconds>              Maximum number of seconds to wait\n"
    "                               for process to terminate.\n"
    "  -file <file>                 Loads options from a file.  The file.\n"
    "                               should contain one option and it's\n"
    "                               if required per line.\n"
    "  -log <file>                  Output errors to a log file.\n");
}

/**
 * Exits with an error message.  If logging is enabled,
 * the message will be printed to the log file.
 *
 * @param format Message format
 * @param ... Variable arguments
 * @return None
 */
static void
fail (const char *format, ...)
{
  FILE *stream = logFile;
  va_list ap;

  /* If no log file, use standard error */
  if (stream == NULL)
    stream = stderr;

  fprintf (stream, "ERROR: ");
  va_start (ap, format);
  vfprintf (stream, format, ap);
  va_end (ap);
  fprintf (stream, "\n");

  if (logFile != NULL)
    fclose (logFile);

  exit (1);
}

/**
 * Writes a message to the log if logging is enabled.
 *
 * @param format Message format
 * @param ... Variable arguments
 * @return None
 */
static void
log_message (const char *format, ...)
  {
    va_list ap;
    if (logFile != NULL)
      {
        fprintf (logFile, "instmon: ");
        va_start (ap, format);
        vfprintf (logFile, format, ap);
        va_end (ap);
        fprintf (logFile, "\n");
      }
}

/**
 * Strips quotes from a string.
 *
 * @param s String to strip quotes from
 * @return Newly malloced string without quotes.  The client is responsible
 * for freeing this string.
 */
static char *
trim_argument (const char *s)
{
  char *stripped;
  char *ptr;
  int count;

  if (s == NULL)
    return NULL;

  stripped = malloc (strlen (s) + 1);
  if (!stripped)
    fail ("trim_argument: Out of memory");
  while ((*s == '\"') || (*s == ' ') || (*s == '\t'))
    s++;

  strcpy (stripped, s);
  count = strlen (stripped);
  ptr = stripped + count - 1;
  while ((*ptr == '\"') || (*ptr == ' ') || (*ptr == '\t'))
    {
      *ptr = '\0';
      ptr--;
    }

  return stripped;
}

/**
 * Replaces characters in a string.
 *
 * @param s String to replace
 * @param in Character to replace
 * @param out Replacement character
 * @return Replaced string
 */
static char *
replace_chars (char *s, char in, char out)
{
  int index;
  int len = strlen (s);
  for (index = 0; index < len; index ++)
  {
    if (s[index] == in)
    {
      s[index] = out;
    }
  }

  return s;
}

/**
 * Concats a set of strings together
 *
 * @param s First string
 * @param ... Other strings.  Use NULL argument to indicate the end of the
 * string list.
 * @retval Newly malloced string that is the combination of input strings.  The
 * client is responsible for freeing this string.
 */
static char *
concat (const char *s, ...)
{
  va_list ap;
  int n;
  const char *ss;
  char *result;

  n = strlen (s);
  va_start (ap, s);
  for (ss = va_arg (ap, const char *); ss; ss = va_arg (ap, const char *))
    n += strlen (ss);
  va_end (ap);

  result = malloc (n + 1);
  if (!result)
    fail ("concat: Out of memory");
  strcpy (result, s);
  va_start (ap, s);
  for (ss = va_arg (ap, const char *); ss; ss = va_arg (ap, const char *))
    strcat (result, ss);
  va_end (ap);

  return result;
}

/**
 * Returns the number of files and directories in a directory.
 *
 * @param path Full path to the directory
 * @return Number of files and directories in the directory.
 */
int
directory_size (const char* path)
{
  int count = 0;
  struct dirent *entry;

  DIR *dir = opendir (path);
  if (dir == NULL)
    {
      fail ("[directory_size] Size could not be obtained for: %s - %s", path, strerror (errno));
    }

  while ((entry = readdir (dir)))
    {
      /* Skip special entries.  */
      if (strcmp (entry->d_name, ".") == 0)
        continue;
      if (strcmp (entry->d_name, "..") == 0)
        continue;

      count ++;
    }

    closedir (dir);

    return count;
}

/**
 * Test whether a file or directory exists.
 *
 * @param filename Full path to the file or directory
 * @return 0 if file does not exists
 */
static int
file_exists (const char *filename)
{
#ifdef _WIN32
  return (GetFileAttributes (filename) != INVALID_FILE_ATTRIBUTES);
#else
  struct stat s;
  return (stat (filename, &s) == 0);
#endif
}

/**
 * Makes a path by creating all required directories.
 *
 * @param directory Full path to the directory
 * @param mode Permissions (not used on Windows)
 * @return 0 on success
 */
static int
make_path (const char *directory, mode_t mode)
{
  int result = 0;
  char *path;
  char *start;
  char *sp;

  path = strdup (directory);
  if (path == NULL)
    fail ("[make_path] Out of memory");
  start = path;

  while ((sp = strchr (start, DIRSEPSTR[0])) != 0)
    {
      char old = *sp;
      *sp = '\0';
      if ((strlen (path) != 0) && !file_exists (path))
        {
#ifdef _WIN32
          if (mkdir (path) != 0)
#else
          if (mkdir (path, mode) != 0)
#endif
            {
              log_message ("[make_path] Failed to create directory: %s - %s", path, strerror (errno));
              result = -1;
              break;
            }
        }
      *sp = old;
      start = sp + 1;
    }
  if (!file_exists (directory))
#ifdef _WIN32
    if (mkdir (path) != 0)
#else
    if (mkdir (path, mode) != 0)
#endif
      {
        log_message ("[make_path] Failed to create directory: %s - %s", path, strerror (errno));
        result = -1;
      }

  free (path);
  return result;
}

/**
 * Deletes a directory and all child files and directories.
 * Does not move the directory to the recycle bin or trash.
 *
 * @param directory Full path to the directory
 * @param emptyOnly 1 to only delete directories that are empty
 * @return 0 on success
 */
static int
delete_directory (const char *directory, int emptyOnly)
{
  int result = 0;
  struct dirent *entry;
#ifdef _WIN32
  SHFILEOPSTRUCT fileop;
  char *szDirectory;
  int len;
#endif

  if (directory == NULL)
    fail ("[delete_directory] No directory provided");

  DIR *dir = opendir (directory);
  if (dir == NULL)
    {
      return -1;
    }

  while ((entry = readdir (dir)))
    {
      struct stat s;
      char *child;

      /* Skip special entries.  */
      if (strcmp (entry->d_name, ".") == 0)
        continue;
      if (strcmp (entry->d_name, "..") == 0)
        continue;

      child = concat (directory, DIRSEPSTR, entry->d_name,
		      (const char *) NULL);

#ifdef _WIN32
      if (stat (child, &s) != 0)
#else
      /* Use lstat on Linux so symbolic links are not followed */
      if (lstat (child, &s) != 0)
#endif
        {
          free (child);
          continue;
        }

      /* Directory */
      if (S_ISDIR (s.st_mode))
        {
          if (delete_directory (child, emptyOnly) != 0)
            {
              result = -1;
              break;
            }
        }
      /* File */
      else
        {
          if (emptyOnly)
            {
              log_message ("[delete_directory] Directory not empty: %s", directory);
              result = -1;
              break;
            }
          if (unlink (child) != 0)
            {
              log_message ("[delete_directory] Failed to delete file: %s", child);
              result = -1;
              break;
            }
        }

      free (child);
    }

  closedir (dir);
  if (result == 0)
    {
#ifdef _WIN32
      /* SHFileOperation requires double null terminated string */
      len = strlen (directory) + 2;
      szDirectory = (char*)malloc (len);
      memset (szDirectory, 0, len);
      strcpy (szDirectory, directory);

      fileop.hwnd = NULL; /* No window */
      fileop.wFunc = FO_DELETE; /* Delete operation */
      fileop.pFrom = szDirectory; /* Full path to directory */
      fileop.pTo = NULL; /* No destination */
      fileop.fFlags = FOF_NOCONFIRMATION | FOF_NOERRORUI | FOF_SILENT; /* Silent operation */
      fileop.fAnyOperationsAborted = FALSE; /* No aborts */
      fileop.lpszProgressTitle = NULL; /* No progress UI title */
      fileop.hNameMappings = NULL; /* No mappings */

      result = SHFileOperation (&fileop);
      free (szDirectory);
#else
      result = rmdir (directory);
#endif
    }
  return result;
}

/**
 * Deletes a set of directories.
 *
 * @param directories Comma separated full paths of directories to delete
 * @param emptyOnly 1 to only delete directory if it is empty
 * @return 0 on success.  If the result is not 0 then some directories may
 * have been deleted, but at least one failed to be deleted.
 */
int
delete_directories (char *directories, int emptyOnly)
{
  int result = 0;
  int del_result;
  char *argument = strtok (directories, ",");
  while (argument != NULL)
    {
        del_result = delete_directory (argument, emptyOnly);
        if (del_result != 0)
          {
            result = -1;
          }
      argument = strtok (NULL, ",");
    }

  return result;
}

/**
 * Waits for a process to terminate.
 *
 * @param pid Process identifier (PID)
 * @param max Maximum number of seconds to wait for process termination
 * @return 0 on success, -1 if time out waiting for process
 */
int
wait_for_pid (pid_t pid, int max)
{
  int result = 0;

  if (pid != -1)
    {
#ifdef _WIN32
      HANDLE handle = OpenProcess (SYNCHRONIZE, FALSE, pid);
      if (handle != NULL)
      {
        if (WaitForSingleObject (handle, max * 1000) == WAIT_TIMEOUT)
          result = -1;
        CloseHandle (handle);
      }
#else
      int num_seconds = 0;

      while (kill (pid, 0) != -1)
        {
          sleep (1);
          if ((max != -1) && (num_seconds >= max))
            {
              result = -1;
              break;
            }
        }
#endif
    }

  return result;
}

/**
 * Prints an operating specific property value.
 *
 * @param name Property name
 */
void
print_os_property(char *name)
{
#ifdef _WIN32
  OSVERSIONINFOEX osvi;
  BOOL result;
#endif

#ifdef _WIN32
  ZeroMemory(&osvi, sizeof(OSVERSIONINFOEX));
  osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);

  /* Windows major version */
  if (strcmp (name, "WIN_MAJOR_VERSION") == 0)
    {
      result = GetVersionEx ((OSVERSIONINFO*) &osvi);
      if (result != 0)
        printf("%lu\n", osvi.dwMajorVersion);
    }
  /* Windows minor version */
  else if (strcmp (name, "WIN_MINOR_VERSION") == 0)
    {
      result = GetVersionEx ((OSVERSIONINFO*) &osvi);
      if (result != 0)
        printf("%lu\n", osvi.dwMinorVersion);
    }
#endif
}

#ifdef _WIN32
/**
 * Gets a registry key and subkey from a fully qualified
 * registry path.
 *
 * @param keyPath Fully qualified path of registry key.  This parameter is
 * modified on return.
 * @param key Filled with the registry key.
 * @param subkey Filled with the registry subkey.
 * @return None
 */
void
get_registry_key (char *keyPath, HKEY *key, char **subkey)
{
  char *ptr;
  ptr = strpbrk (keyPath, DIRSEPSTR);
  *ptr = '\0';
  while (*++ptr == DIRSEPSTR[0]);
   *subkey = ptr;

  if (strcmp (keyPath, "HKEY_CLASSES_ROOT") == 0)
    {
      *key = HKEY_CLASSES_ROOT;
    }
  else if (strcmp (keyPath, "HKEY_CURRENT_CONFIG") == 0)
    {
      *key = HKEY_CURRENT_CONFIG;
    }
  else if (strcmp (keyPath, "HKEY_CURRENT_USER") == 0)
    {
      *key = HKEY_CURRENT_USER;
    }
  else if (strcmp (keyPath, "HKEY_LOCAL_MACHINE") == 0)
    {
      *key = HKEY_LOCAL_MACHINE;
    }
  else if (strcmp (keyPath, "HKEY_USERS") == 0)
    {
      *key = HKEY_USERS;
    }
  else
    {
      *key = 0;
      log_message ("[get_registry_key] Unknown key");
    }

  return;
}

char*
get_registry_value (char *keyName, const char *name)
{
  HKEY key, hKey;
  char *subkey;
  LONG result;
  DWORD type;
  DWORD data;
  DWORD dataSize;
  char *value = NULL;

  get_registry_key (keyName, &key, &subkey);
  result = RegOpenKeyEx (key, subkey, 0, KEY_READ, &hKey);

  dataSize = 0;
  RegQueryValueEx (hKey, name, 0, &type, NULL, &dataSize);

  if (type == REG_DWORD)
    {
      dataSize = sizeof (DWORD);
      result = RegQueryValueEx (hKey, name, 0, NULL, (LPBYTE)&data, &dataSize);
      if (result == ERROR_SUCCESS)
        {
          value = malloc (255);
          _ultoa (data, value, 10);
        }
    }
  else if ((type == REG_EXPAND_SZ) || (type == REG_SZ))
    {
      value = malloc (dataSize + 1);
      memset (value, dataSize + 1, 0);
      result = RegQueryValueEx (hKey, name, 0, NULL, (LPBYTE)value, &dataSize);
    }

  RegCloseKey (hKey);

  return value;
}

/**
 * Sets a registry value.
 *
 * @param keyName Fully qualified registry key.  This parameter is modified.
 * @param name Name of registry value
 * @param value Value
 * @param type Type of value (string or dword)
 * @return 0 on success
 */
int
set_registry_value (char *keyName, const char *name, const char *value, const char *type)
{
  int ret = 0;
  LONG result;
  HKEY key;
  HKEY hKey;
  char *subkey;
  DWORD dispos;
  DWORD data;

  get_registry_key (keyName, &key, &subkey);
  result = RegCreateKeyEx (key, subkey, 0L, NULL, REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, NULL, &hKey, &dispos);
  if (result != ERROR_SUCCESS)
    {
      log_message ("[set_registry_value] Failed to create key: %s, %s", keyName, name);
      return -1;
    }

  if (strcmp (type, "string") == 0)
    {
      result = RegSetValueEx (hKey, name, 0L, REG_SZ, (BYTE *)value, strlen(value) + 1);
      if (result != ERROR_SUCCESS)
        {
          log_message ("[set_registry_value] Failed to set value: %s, %s, %s, %s", keyName, name, value, type);
          result = -1;
        }
    }
  else if (strcmp (type, "dword") == 0)
    {
      data = atol (value);
      result = RegSetValueEx (hKey, name, 0L, REG_DWORD, (BYTE *)&data, sizeof(DWORD));
      if (result != ERROR_SUCCESS)
        {
          log_message ("[set_registry_value] Failed to set value: %s, %s, %s, %s", keyName, name, value, type);
          result = -1;
        }
    }
  else
    {
      log_message ("[set_registry_value] Unknown value type: %s", type);
      ret = -1;
    }

  RegCloseKey (hKey);

  return ret;
}

/**
 * Deletes a registry value.
 *
 * @param keyName Fully path of registry key.  This parameter is modified.
 * @param name Value name
 * @return 0 on success
 */
int
delete_registry_value (char *keyName, const char *name)
{
  int ret = 0;
  LONG result;
  HKEY key;
  HKEY hKey;
  char *subkey;

  if ((keyName == NULL) || (name == NULL))
    return -1;

  get_registry_key (keyName, &key, &subkey);
  result = RegOpenKeyEx (key, subkey, 0L, KEY_ALL_ACCESS, &hKey);
  if (result != ERROR_SUCCESS)
    {
      log_message ("[delete_registry_value] Failed to open key: %s", keyName);
      return -1;
    }
  result = RegDeleteValue (hKey, name);
  if (result != ERROR_SUCCESS)
    {
      log_message ("[delete_registry_value] Failed to delete value: %s, %s", keyName, name);
      ret = -1;
    }

  RegCloseKey (hKey);

  return ret;
}

/**
 * Deletes a registry key.
 *
 * @param keyName Full path to the registry key.  This parameter is modified.
 * @return 0 on success
 */
int
delete_registry_key (char *keyName)
{
  LONG result;
  HKEY key;
  char *subkey;

  if (keyName == NULL)
    return -1;

  get_registry_key (keyName, &key, &subkey);
  result = RegDeleteKey (key, subkey);
  if (result != ERROR_SUCCESS)
    {
      log_message ("[delete_registry_key] Failed to delete key: %s", keyName);
      return -1;
    }

  return 0;
}

/**
 * Returns the path to a special folder on Windows.
 *
 * @param clsid CLSID of folder
 * @param path Buffer for path, must be MAX_PATH in size
 * @return 0 on success
 */
int
get_special_folder(int clsid, char *path) {
  HRESULT status;
  status = SHGetFolderPath (NULL, clsid, NULL, 0, path);
  if (SUCCEEDED (status))
    return 0;
  else
    return -1;
}
#endif

/**
 * Creates a programs short-cut.
 *
 * @param directory Path to short-cut in folder
 * @param linkName Name for short-cut
 * @param targetFile Target file for short-cut
 * @param targetArguments Arguments for short-cut (Supported on Windows only)
 * @param description Description for short-cut or NULL (Supported on Windows only)
 * @param showMode Show mode for short-cut or -1 (Supported on Windows only)
 * @param workingDirectory Working directory for short-cut or NULL (Supported on Windows only)
 * @param iconFile File containing icon or NULL (Supported on Windows only))
 * @param iconIndex Index of icon in icon file or -1 (Supported on Windows only)
 * @return 0 on success
 */
int
create_shortcut (const char *directory, const char *linkName, const char *targetFile,
  const char *targetArguments, const char *description, int showMode, const char
  *workingDirectory, const char *iconFile, int iconIndex)
{
  int result = 0;
  char *linkPath;
#ifdef _WIN32
  HRESULT status;
  IShellLink *shellLink;
  IPersistFile *persistFile;
  WORD wideLinkPath[MAX_PATH];

  /* Initialize COM */
  CoInitialize (NULL);

  /** Short-cut path */
  if (!file_exists (directory))
    make_path (directory, 0777);
  linkPath = concat (directory, DIRSEPSTR, linkName, ".lnk", NULL);

  /* Create the short-cut */
  status = CoCreateInstance (&CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, &IID_IShellLink, (void *)&shellLink);
  if (SUCCEEDED (status))
    {
      /* Note, setPath() will automatically quote long paths */
      shellLink->lpVtbl->SetPath (shellLink, targetFile);
      shellLink->lpVtbl->SetArguments (shellLink, targetArguments);
      if (strlen (description) > 0)
        shellLink->lpVtbl->SetDescription (shellLink, description);
      if (showMode >= 0)
        shellLink->lpVtbl->SetShowCmd (shellLink, showMode);
      if (strlen (workingDirectory) > 0)
        shellLink->lpVtbl->SetWorkingDirectory (shellLink, workingDirectory);
      if ((iconFile != NULL) && (iconIndex >= 0))
        shellLink->lpVtbl->SetIconLocation (shellLink, iconFile, iconIndex);

      /* Persist the short-cut to the file */
      status = shellLink->lpVtbl->QueryInterface (shellLink, &IID_IPersistFile, (void **)&persistFile);
      if (SUCCEEDED (status))
        {
          MultiByteToWideChar (CP_ACP, 0, linkPath, -1, wideLinkPath, MAX_PATH);
          status = persistFile->lpVtbl->Save (persistFile, wideLinkPath, TRUE);
          if (SUCCEEDED (status))
            {
              persistFile->lpVtbl->SaveCompleted (persistFile, wideLinkPath);
            }
          else
            {
              log_message ("[create_shortcut] Failed to save shortcut file: %s", linkPath);
              result = -1;
            }
        }
      else
        {
          log_message ("[create_shortcut] Failed to query persist file interface");
          result = -1;
        }

      persistFile->lpVtbl->Release (persistFile);
    }
  else
    {
      log_message ("[create_shortcut] Failed to create shell link object");
      result = -1;
    }

    shellLink->lpVtbl->Release (shellLink);
    free (linkPath);

  CoUninitialize ();
#else
  /** Short-cut path */
  if (!file_exists (directory))
    make_path (directory, 0777);
  linkPath = concat (directory, DIRSEPSTR, linkName, NULL);
  result = symlink (targetFile, linkPath);
  free (linkPath);
#endif

  return result;
}

#ifdef _WIN32
/**
 * Returns the CLSID for a named folder.
 *
 * @param clsid CLSID name
 * @return CLSID or -1 for unknown CLSID name
 */
int
get_clsid (const char* clsid)
{
  if (clsid == NULL)
    return -1;

  if (strcmp (clsid, "CSIDL_ADMINTOOLS") == 0)
    return CSIDL_ADMINTOOLS;
  else if (strcmp (clsid, "CSIDL_ALTSTARTUP") == 0)
    return CSIDL_ALTSTARTUP;
  else if (strcmp (clsid, "CSIDL_APPDATA") == 0)
    return CSIDL_APPDATA;
  else if (strcmp (clsid, "CSIDL_BITBUCKET") == 0)
    return CSIDL_BITBUCKET;
  else if (strcmp (clsid, "CSIDL_STARTMENU") == 0)
    return CSIDL_STARTMENU;
  else if (strcmp (clsid, "CSIDL_CDBURN_AREA") == 0)
    return CSIDL_CDBURN_AREA;
  else if (strcmp (clsid, "CSIDL_COMMON_ADMINTOOLS") == 0)
    return CSIDL_COMMON_ADMINTOOLS;
  else if (strcmp (clsid, "CSIDL_COMMON_ALTSTARTUP") == 0)
    return CSIDL_COMMON_ALTSTARTUP;
  else if (strcmp (clsid, "CSIDL_COMMON_APPDATA") == 0)
    return CSIDL_COMMON_APPDATA;
  else if (strcmp (clsid, "CSIDL_COMMON_DESKTOPDIRECTORY") == 0)
    return CSIDL_COMMON_DESKTOPDIRECTORY;
  else if (strcmp (clsid, "CSIDL_COMMON_DOCUMENTS") == 0)
    return CSIDL_COMMON_DOCUMENTS;
  else if (strcmp (clsid, "CSIDL_COMMON_FAVORITES") == 0)
    return CSIDL_COMMON_FAVORITES;
  else if (strcmp (clsid, "CSIDL_COMMON_MUSIC") == 0)
    return CSIDL_COMMON_MUSIC;
  else if (strcmp (clsid, "CSIDL_COMMON_OEM_LINKS") == 0)
    return CSIDL_COMMON_OEM_LINKS;
  else if (strcmp (clsid, "CSIDL_COMMON_PICTURES") == 0)
    return CSIDL_COMMON_PICTURES;
  else if (strcmp (clsid, "CSIDL_COMMON_PROGRAMS") == 0)
    return CSIDL_COMMON_PROGRAMS;
  else if (strcmp (clsid, "CSIDL_COMMON_STARTMENU") == 0)
    return CSIDL_COMMON_STARTMENU;
  else if (strcmp (clsid, "CSIDL_COMMON_STARTUP") == 0)
    return CSIDL_COMMON_STARTUP;
  else if (strcmp (clsid, "CSIDL_COMMON_TEMPLATES") == 0)
    return CSIDL_COMMON_TEMPLATES;
  else if (strcmp (clsid, "CSIDL_COMMON_VIDEO") == 0)
    return CSIDL_COMMON_VIDEO;
  else if (strcmp (clsid, "CSIDL_COMPUTERSNEARME") == 0)
    return CSIDL_COMPUTERSNEARME;
  else if (strcmp (clsid, "CSIDL_CONNECTIONS") == 0)
    return CSIDL_CONNECTIONS;
  else if (strcmp (clsid, "CSIDL_CONTROLS") == 0)
    return CSIDL_CONTROLS;
  else if (strcmp (clsid, "CSIDL_COOKIES") == 0)
    return CSIDL_COOKIES;
  else if (strcmp (clsid, "CSIDL_DESKTOP") == 0)
    return CSIDL_DESKTOP;
  else if (strcmp (clsid, "CSIDL_DESKTOPDIRECTORY") == 0)
    return CSIDL_DESKTOPDIRECTORY;
  else if (strcmp (clsid, "CSIDL_DRIVES") == 0)
    return CSIDL_DRIVES;
  else if (strcmp (clsid, "CSIDL_FAVORITES") == 0)
    return CSIDL_FAVORITES;
  else if (strcmp (clsid, "CSIDL_FONTS") == 0)
    return CSIDL_FONTS;
  else if (strcmp (clsid, "CSIDL_HISTORY") == 0)
    return CSIDL_HISTORY;
  else if (strcmp (clsid, "CSIDL_INTERNET") == 0)
    return CSIDL_INTERNET;
  else if (strcmp (clsid, "CSIDL_INTERNET_CACHE") == 0)
    return CSIDL_INTERNET_CACHE;
  else if (strcmp (clsid, "CSIDL_LOCAL_APPDATA") == 0)
    return CSIDL_LOCAL_APPDATA;
  else if (strcmp (clsid, "CSIDL_MYMUSIC") == 0)
    return CSIDL_MYMUSIC;
  else if (strcmp (clsid, "CSIDL_MYPICTURES") == 0)
    return CSIDL_MYPICTURES;
  else if (strcmp (clsid, "CSIDL_MYVIDEO") == 0)
    return CSIDL_MYVIDEO;
  else if (strcmp (clsid, "CSIDL_NETHOOD") == 0)
    return CSIDL_NETHOOD;
  else if (strcmp (clsid, "CSIDL_NETWORK") == 0)
    return CSIDL_NETWORK;
  else if (strcmp (clsid, "CSIDL_PERSONAL") == 0)
    return CSIDL_PERSONAL;
  else if (strcmp (clsid, "CSIDL_PRINTERS") == 0)
    return CSIDL_PRINTERS;
  else if (strcmp (clsid, "CSIDL_PRINTHOOD") == 0)
    return CSIDL_PRINTHOOD;
  else if (strcmp (clsid, "CSIDL_PROFILE") == 0)
    return CSIDL_PROFILE;
  else if (strcmp (clsid, "CSIDL_PROGRAM_FILES") == 0)
    return CSIDL_PROGRAM_FILES;
  else if (strcmp (clsid, "CSIDL_PROGRAM_FILESX86") == 0)
    return CSIDL_PROGRAM_FILESX86;
  else if (strcmp (clsid, "CSIDL_PROGRAM_FILES_COMMON") == 0)
    return CSIDL_PROGRAM_FILES_COMMON;
  else if (strcmp (clsid, "CSIDL_PROGRAM_FILES_COMMONX86") == 0)
    return CSIDL_PROGRAM_FILES_COMMONX86;
  else if (strcmp (clsid, "CSIDL_PROGRAMS") == 0)
    return CSIDL_PROGRAMS;
  else if (strcmp (clsid, "CSIDL_RECENT") == 0)
    return CSIDL_RECENT;
  else if (strcmp (clsid, "CSIDL_RESOURCES") == 0)
    return CSIDL_RESOURCES;
  else if (strcmp (clsid, "CSIDL_RESOURCES_LOCALIZED") == 0)
    return CSIDL_STARTMENU;
  else if (strcmp (clsid, "CSIDL_RESOURCES_LOCALIZED") == 0)
    return CSIDL_STARTMENU;
  else if (strcmp (clsid, "CSIDL_STARTMENU") == 0)
    return CSIDL_STARTMENU;
  else if (strcmp (clsid, "CSIDL_STARTUP") == 0)
    return CSIDL_STARTUP;
  else if (strcmp (clsid, "CSIDL_SYSTEM") == 0)
    return CSIDL_SYSTEM;
  else if (strcmp (clsid, "CSIDL_SYSTEMX86") == 0)
    return CSIDL_SYSTEMX86;
  else if (strcmp (clsid, "CSIDL_TEMPLATES") == 0)
    return CSIDL_TEMPLATES;
  else if (strcmp (clsid, "CSIDL_WINDOWS") == 0)
    return CSIDL_WINDOWS;

  return -1;
}

#endif

/**
 * Runs a program with elevated rights.
 *
 * @param path Path to the program
 * @param arguments Program arguments
 */
int
run_admin (const char *path, const char *arguments)
{
#ifdef _WIN32
  HINSTANCE result;
  result = ShellExecute (NULL, "runas", path, arguments, NULL, SW_NORMAL);
  if (result >= (HINSTANCE)32)
	  return 0;
  else
	  return -1;
#else
  return -1;
#endif
}

/**
 * Returns an argument in a string delimited by separators.
 * Calling the first time with the input string returns the first argument.
 * Calling subsequent times with NULL returns the next argument in the
 * string.
 * The input string is modified.
 *
 * @param arguments Arguments string
 * @return The next argument or NULL if there are not more arguments
 */
char *
get_argument(char *arguments)
{
    static char *current;
    char *pos, *ret;

    if (arguments != NULL)
        current = arguments;

    if (current == NULL)
        return current;

    ret = current;
    pos = strpbrk (current, ",");
    if (pos == NULL)
      {
        current = NULL;
      }
    else
      {
        *pos = '\0';
        current = pos+1;
      }
    return ret;
}


/**
 * Internal routine to read command line option.
 *
 * @param argv Command line arguments
 * @param argc Number of command line arguments
 * @param start Starting index of command line arguments
 * @param name Name of command line argument
 * @param argument Buffer to fill with option argument or NULL
 * @return 0 on success
 */
int
internal_get_option (char* argv[], int argc, int start, const char *name, char** argument)
{
  int index;
  const char *arg;
  for (index = start; index < argc; index ++)
    {
      arg = argv[index];
      if (strcmp (name, arg) == 0)
        {
          if (argument != NULL)
            {
              index ++;
              if (index >= argc)
                fail ("%s option requires an argument.", name);
              *argument = trim_argument (argv[index]);
            }
          return 0;
        }
    }

  return -1;
}

/**
 * Reads a command line option and its argument (if required).
 *
 * @param argv Command line arguments
 * @param argc Number of command line arguments
 * @param name Name of command line argument
 * @param argument Buffer to fill with option argument or NULL
 * @return 0 on success
 */
int
get_option (char* argv[], int argc, const char *name, char** argument)
{
  /* Get option from command line */
  if (internal_get_option (argv, argc, 1, name, argument) != 0)
    {
      /* Get option from file */
      return internal_get_option (fargv, fargc, 0, name, argument);
    }

  return 0;
}

/**
 * Reads command line options from a file.
 *
 * @param path Path to the command file
 * @return 0 on success
 */
int
read_file_options (const char *path)
{
  FILE *fp;
  int length;
  char buffer[2048];
  char *sp;
  int index = 0;

  if (path == NULL)
    return -1;

  /* Count the number of options */
  fargc = 0;
  fp = fopen (path, "r");
  if (fp)
    {
      while (fgets (buffer, 2048, fp) != NULL)
        {
          fargc++;
          if (strpbrk (buffer, " \t") != NULL)
            fargc++;
        }
      fclose (fp);
    }
  else
    {
      return -1;
    }

  fargv = (char**) malloc (fargc * sizeof (char*));

  /* Read the options */
  fp = fopen (path, "r");
  if (fp)
    {
      while (fgets (buffer, 2048, fp) != NULL)
        {
          length = strlen (buffer);
          if (buffer[length - 1] == '\n')
            buffer[length - 1] = '\0';

          sp = strpbrk (buffer, " \t");
          if (sp != NULL)
            {
              while ((*sp == ' ') || (*sp == '\t'))
                *sp++ = '\0';
            }

          fargv[index] = malloc (strlen (buffer) + 1);
          strcpy (fargv[index], buffer);
          index ++;
          if (sp != NULL)
            {
              fargv[index] = malloc (strlen (sp) + 1);
              strcpy (fargv[index], sp);
              index ++;
            }
        }
      fclose (fp);
    }
  else
  {
    return -1;
  }

  for (index = 0; index < fargc; index++)
  {
	  sp = fargv[index];
	  length = 0;
  }

  return 0;
}

/**
 * Main entry for program.
 *
 * @param argc Number of command line arguments
 * @param argv Command line arguments
 * @return 0 on success
 */
int
main (int argc, char* argv[])
{
  char *option;
  int result;
  char *sparam1=NULL, *sparam2, *sparam3, *sparam4;
  char *sparam5, *sparam6, *sparam7, *sparam8, *sparam9;
#ifdef _WIN32
  char path[MAX_PATH];
#endif
  int iparam1, iparam2;
  long lparam1;

  /****************************************************************************
   * Show program help
  ****************************************************************************/
  if (get_option (argv, argc, "--help", NULL) == 0)
    {
      usage (stdout);
      exit (0);
    }
  /****************************************************************************
   * Read command line options from file
  ****************************************************************************/
  if (get_option (argv, argc, "-file", &option) == 0)
    {
      result = read_file_options (option);
      free (option);
      if (result == -1)
        fail ("Failed to read options file: %s.", option);
    }
  /****************************************************************************
   * Enable logging
  ****************************************************************************/
  if (get_option (argv, argc, "-log", &option) == 0)
    {
      logFile = fopen (option, "a");
      if (logFile == NULL)
        fail ("Failed to write log file: %s", option);
      free (option);
    }
  /****************************************************************************
   * Wait for process to terminate
  ****************************************************************************/
  if (get_option (argv, argc, "-pid", &option) == 0)
    {
      iparam1 = -1;
      lparam1 = atoi (option);
      free (option);
      if (get_option (argv, argc, "-wait", &option) == 0)
        {
          iparam1 = atoi (option);
        }
      if (wait_for_pid (lparam1, iparam1) != 0)
        fail ("Timed out waiting for process to terminate.");
      free (option);
    }
  /****************************************************************************
   * Remove directories
  ****************************************************************************/
  if (get_option (argv, argc, "-removeDir", &option) == 0)
    {
      result = delete_directories (option, 0);
      if (result != 0)
        fail ("Failed to delete directories: %s", option);
      else
        log_message ("-removeDir %s", option);
      free (option);
    }
  /****************************************************************************
   * Remove directories if they are empty
  ****************************************************************************/
  if (get_option (argv, argc, "-removeEmptyDir", &option) == 0)
    {
      result = delete_directories (option, 1);
      if (result != 0)
        fail ("Failed to delete empty directories: %s", option);
      else
        log_message ("-removeEmptyDir %s", option);
      free (option);
    }
  /****************************************************************************
   * Set registry value
  ****************************************************************************/
  if (get_option (argv, argc, "-regSetValue", &option) == 0)
    {
#ifdef _WIN32
      sparam1 = get_argument (option);
      sparam2 = get_argument (NULL);
      sparam3 = get_argument (NULL);
      sparam4 = get_argument (NULL);
      result = set_registry_value (sparam1, sparam2, sparam3, sparam4);
      if (result != 0)
        fail ("Failed to set registry value.");
      else
        log_message ("-regSetValue %s, %s, %s, %s", sparam1, sparam2,
          sparam3, sparam4);
      free (option);
#else
      fail ("-regSetValue is only supported on Windows.");
#endif
    }
  /****************************************************************************
   * Get registry value
  ****************************************************************************/
  if (get_option (argv, argc, "-regGetValue", &option) == 0)
    {
#ifdef _WIN32
      sparam1 = get_argument (option);
      sparam2 = get_argument (NULL);
      sparam3 = get_registry_value (sparam1, sparam2);
      if (sparam3)
        puts (sparam3);
      else
        fail ("Failed to get registry value %s, %s.", sparam1, sparam2);
      free (option);
#else
      fail ("-regSetValue is only supported on Windows.");
#endif
    }
  /****************************************************************************
   * Delete registry value
  ****************************************************************************/
  if (get_option (argv, argc, "-regDeleteValue", &option) == 0)
    {
#ifdef _WIN32
      sparam1 = get_argument (option);
      sparam2 = get_argument (NULL);
      result = delete_registry_value (sparam1, sparam2);
      if (result != 0)
        fail ("Failed to delete registry value.");
      else
        log_message ("-regDeleteValue %s, %s", sparam1, sparam2);
      free (option);
#else
      fail ("-regDeleteValue is only supported on Windows.");
#endif
    }
  /****************************************************************************
   * Delete registry key
  ****************************************************************************/
  if (get_option (argv, argc, "-regDeleteKey", &option) == 0)
    {
#ifdef _WIN32
      result = delete_registry_key (option);
      if (result != 0)
        fail ("Failed to delete registry key.");
      else
        log_message ("-regDeleteKey %s", option);
      free (option);
#else
      fail ("-regDeleteKey is only supported on Windows.");
#endif
    }
  /****************************************************************************
   * Run as administrator
  ****************************************************************************/
  if (get_option (argv, argc, "-runAdmin", &option) == 0)
    {
#ifdef _WIN32
      sparam1 = get_argument (option);
      sparam2 = get_argument (NULL);

      result = -1;
      if ((sparam1 != NULL) && (sparam2 != NULL))
      {
        replace_chars (sparam2, '\'', '"');
        result = run_admin (sparam1, sparam2);
      }
      if (result != 0)
        fail ("Failed to run program.");
      else
        log_message ("-runAdmin %s,%s", sparam1, sparam2);
      free (option);
#else
      fail ("-runAdmin is currently only supported on Windows.");
#endif
    }
  /****************************************************************************
   * Create short-cut
  ****************************************************************************/
  if (get_option (argv, argc, "-createShortcut", &option) == 0)
    {
      sparam1 = get_argument (option);
      sparam2 = get_argument (NULL);
      sparam3 = get_argument (NULL);
      sparam4 = get_argument (NULL);
      sparam5 = get_argument (NULL);
      sparam6 = get_argument (NULL);
      sparam7 = get_argument (NULL);
      sparam8 = get_argument (NULL);
      sparam9 = get_argument (NULL);
      if (strlen (sparam6) != 0)
        iparam1 = atoi (sparam6);
      else
        iparam1 = 0;
      if (strlen (sparam9) != 0)
        iparam2 = atoi (sparam9);
      else
        iparam2 = 0;
      result = create_shortcut (sparam1, sparam2, sparam3, sparam4, sparam5,
        iparam1, sparam7, sparam8, iparam2);
      if (result != 0)
        fail ("Failed to create shortcut.");
      else
        log_message ("-createShortcut %s, %s, %s, %s, %s, %s, %s, %s %s",
          sparam1, sparam2, sparam3, sparam4, sparam5, sparam6, sparam7,
          sparam8, sparam9);
      free (option);
    }
  /****************************************************************************
   * Get special folder
  ****************************************************************************/
  if (get_option (argv, argc, "-getSpecialFolder", &option) == 0)
    {
#ifdef _WIN32
      sparam1 = get_argument (option);
      iparam1 = get_clsid (sparam1);
      result = get_special_folder (iparam1, path);
      free (option);
      if (result != 0)
        fail ("Failed to get special folder: %s", sparam1);
      else
        puts(path);
#else
      fail ("-getSpecialFolder is only supported on Windows.");
#endif
    }
  /****************************************************************************
   * Get property
  ****************************************************************************/
  if (get_option (argv, argc, "-getOsProperty", &option) == 0)
    {
      sparam1 = get_argument (option);
      print_os_property (sparam1);
      free (option);
    }

  if (logFile != NULL)
    fclose (logFile);
  if (fargv != NULL)
    free (fargv);

  return EXIT_SUCCESS;
}




