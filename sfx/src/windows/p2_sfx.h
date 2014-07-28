/* p2_sfx.h - Data sructures used to describe files processed by sfx */

/*************************************************************************
*  Copyright (c) 2014 Mentor Graphics and others.
*  All rights reserved. This program and the accompanying materials
*  are made available under the terms of the Eclipse Public License v1.0
*  which accompanies this distribution, and is available at
*  http://www.eclipse.org/legal/epl-v10.html
*
*  Contributors:
*     Mentor Graphics - initial API and implementation
**************************************************************************/

/* The following macros may be adjusted */
#ifndef LOG_DIRECTORY
  #define LOG_DIRECTORY ".p2_installer"
#endif
#define LOG_FILE "p2_sfx.log"
#define TEMP_PATH_ENV "P2_INSTALLER_TEMP_PATH"
#define BASE_PATH "p2installer."

/* The UNIX_BASE_PATH and WINDOW_BASE_PATH strings are also used
 * in the core_build_windows_p2_sfx.sh script. If the values are changed
 * in this file, they must also be changed in that file */
#define UNIX_BASE_PATH "@UNIX_BASE_PATH@" /* p2_sfx_unix_base_path */
#define WINDOWS_BASE_PATH "@WINDOWS_BASE_PATH@" /* p2_sfx_windows_base_path */
#define SETUP "@SETUP@" /* p2_sfx_setup */

#define GUI_SETUP_COMMAND "setup.exe"
#define CONSOLE_SETUP_COMMAND "setupc.exe"

/* The following data structures provide a very simplistic 
   mechansim to linke binary data to the main program */

/* Each file is described by a file descriptor. The start symbol
   and end symbol are provided by the incbin directive */
typedef struct p2_sfx_file_desc {
    char * file_name;
    unsigned char * file_start_symbol;
    unsigned char * file_end_symbol;
} P2_SFX_FILE_DESC;

/* A list of file descriptors are organized into bundles. Each
   bundle of files can have a path, which is relative to the 
   installation base directory, and each bundle can have any
   number of commands which are passed directly to the host
   operating system. */
   
typedef struct p2_sfx_file_bundle {
    P2_SFX_FILE_DESC * files;
    int num_file_descs;
    char * path;
    char **commands;
    int num_commands;
} P2_SFX_FILE_BUNDLE;

