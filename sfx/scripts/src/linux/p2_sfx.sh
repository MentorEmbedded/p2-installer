#!/bin/sh
########################################################################
#
# p2_sfx.sh
#
# Self extracting script for p2 installer on Linux
#
# Available environment variables include the following:
#   
# P2_INSTALLER_TEMP_PATH [when used, allows user to specify a directory 
#                         where the SFX will extract and start the 
#                         installer] 
#
# P2_INSTALLER_BLOCKSIZE [Can be set to specify a particular 
#                         fs_blocksize]
#
########################################################################

########################################################################
# Subroutines
########################################################################

# Print usage of script
usage () 
{
    echo "Usage: $0 [options]"
    echo ""
    echo "where options include:"
    echo ""
    echo "-? | --help | -help | -h Show this help text"
    echo "-location=[path]         Use the specified install location."
    echo "-console                 Perform a console installation"
    echo "-silent                  Perform a silent installation using defaults."
    echo "-nosplash                Do not display splash screen."
    echo "-x [path]                Extract installer to [path] but do not start installer."
    echo ""
    exit 1;
}

# Start of main

timestamp=`date +%Y%m%d%H%M%S`
base_log_dir="$HOME/.p2_installer"
timestamp_log_dir="$base_log_dir/logs/$timestamp"
log_file="/p2_sfx.log"
log_output="$timestamp_log_dir$log_file"

if test ! -d "$timestamp_log_dir";
then
    mkdir -p "$timestamp_log_dir"
fi

# Check for help
if test "$1" = "-?" || \
   test "$1" = "--help" || \
   test "$1" = "-help" || \
   test "$1" = "-h";
then
   usage
fi

# Process arguments
extract_only="false"
silent="false"
suppress_errors="true"
while test $# -ge 1; do
    if test "$1" = "-x";
    then
        output_dir="$2"
        if test -z "$output_dir" || test ! -d "$output_dir";
        then
           mkdir -p "$output_dir"
        fi
        output_dir=$(readlink -e "$output_dir")
        install_dir_base="$output_dir"
        extract_only="true"
        shift
        shift
    elif test "$1" = "-install.silent" || test "$1" = "-silent";
    then
        suppress_errors="false"
        silent="true"
        args="-install.silent $args"
        if test -z "$nosplash";
        then
            args="-nosplash $args"
            nosplash="true"
        fi
        shift
    elif test "$1" = "-install.console" || test "$1" = "-console";
    then
        suppress_errors="false"
        args="-install.console $args"
        if test -z "$nosplash"
        then
            args="-nosplash $args"
            nosplash="true"
        fi
        shift
    elif test "$1" = "-nosplash";
    then
        if test -z "$nosplash"
        then
            args="-nosplash $args"
            nosplash="true"
        fi
        shift
    elif test `awk -v VAR="$1" 'BEGIN { print match(VAR, "location") }'` -gt 0;
    then
        args="-install.location=`echo "$1" | cut -d'=' -f2` $args"
        shift 
    else
        args="$1 $args"
        shift
    fi
done

#@PREPROCESS@

# Find a directory to perform the extraction

using_home_dir="false"
# Validate the install_dir_base directory
if test -z "$install_dir_base";
then
    # check for potential installation directories; Available space
    if test ! -z "$P2_INSTALLER_TEMP_PATH";
    then
        if test ! -d "$P2_INSTALLER_TEMP_PATH";
        then
            mkdir -p "$P2_INSTALLER_TEMP_PATH"
        fi
        install_dir_base=$(readlink -e "$P2_INSTALLER_TEMP_PATH")
    elif test ! -z "$TMP" && test -d "$TMP";
    then
        install_dir_base="$TMP"
    elif test -d /tmp;
    then
        install_dir_base=/tmp
    else
        if test ! -z "$HOME" && test -d "$HOME";
        then
            install_dir_base="$HOME"
            using_home_dir="true"
        fi
    fi
fi

if test -z "$install_dir_base";
then
    printf "Error: directory to extract installer not found\n" >> ${log_output}
    printf "$0 failed to install.\n"  >> ${log_output}
    printf "Try setting P2_INSTALLER_TEMP_PATH environment variable to a valid path and restart.\n" >> ${log_output}
    if test "$silent" = "false";
    then
        printf "Error: directory to extract installer not found\n"
        printf "$0 failed to install.\n"
        printf "Try setting P2_INSTALLER_TEMP_PATH environment variable to a valid path and restart.\n"
    fi
    exit 1;
fi

install_dir_base=$(readlink -e "$install_dir_base")

this_script=$(readlink -e "$0")

# Find out if there is sufficient space to extract. 
# Some tmp directories are on a partitions without a lot of disk space
if test -z "$P2_INSTALLER_BLOCKSIZE";
then
    fs_blocksize=1024
else
    fs_blocksize="$P2_INSTALLER_BLOCKSIZE"
fi
#VAR=4 means the 4th column of the output returned from df
available_install_dir_blocks=`df -P --block-size=$fs_blocksize "$install_dir_base" 2>/dev/null | tail -n 1 | awk -v VAR=4 "{print \\$VAR}"`
if test -z $available_install_dir_blocks;
then
    available_install_dir_blocks=0
fi

archive_blocks=$(expr $ARCHIVE_BYTES / $fs_blocksize + $fs_blocksize)

if test $available_install_dir_blocks -gt  $archive_blocks;
then
    available_fs_blocks=$available_install_dir_blocks
else
    available_fs_blocks="INVALID"
fi

if test "$available_fs_blocks" = "INVALID";
then
    # Get the name of the file system to print the error message
    # VAR=1 is the first column returned from df
    file_system=`df -P --block-size=$fs_blocksize "$install_dir_base" 2>/dev/null | tail -n 1 | awk -v VAR=1 "{print \\$VAR}"`

    printf "Error: Insufficient disk space available in $install_dir_base (on the \"$file_system\" filesystem).\n" >> ${log_output}
    printf "Please free up sufficient disk space (at least $ARCHIVE_BYTES bytes) and restart the installer. Note that you can also set the P2_INSTALLER_TEMP_PATH environment variable to point to a filesystem of your choosing: P2_INSTALLER_TEMP_PATH=<path>\n" >> ${log_output}
    if test "$silent" = "false";
    then
        printf "Error: Insufficient disk space available in $install_dir_base (on the \"$file_system\" filesystem).\n" 
        printf "Please free up sufficient disk space (at least $ARCHIVE_BYTES bytes) and restart the installer. Note that you can also set the P2_INSTALLER_TEMP_PATH environment variable to point to a filesystem of your choosing: P2_INSTALLER_TEMP_PATH=<path>\n"
    fi
    exit 1;
fi

# Create work directory
if test "$extract_only" = "false";
then
    random_string=`cat /dev/urandom | tr -cd 'a-f0-9' | head -c 8`
    work_dir="$install_dir_base/$(basename "$this_script")_sfx.$random_string"
    mkdir -p "$work_dir"
else
    work_dir="$install_dir_base"
fi

# make sure that work_dir was created
if test -z "$work_dir" || test ! -d "$work_dir";
then
    printf "Unable to create work directory: $work_dir\n" >> ${log_output}
    if test "$silent" = "false";
    then
        printf "Unable to create work directory: $work_dir\n"
    fi
    exit 1;
fi

# Set up trap for interrupted install cleanup
# Use man -a signal to find the meaning of each value
# 1 -> SIGHUP (hangup of term or process)
# 2 -> SIGINT (keyboard interrupt)
# 3 -> SIGQUIT (keyboard quit)
# 4 -> SIGILL (illegal instruction)
# 6 -> SIGABRT (Abort signal)
# 9 -> SIGKILL (Kill signal)
# 11 -> SIGSEGV (Invalid memory ref)
# 13 -> SIGPIPE (Broken pipe)
# 14 -> SIGALRM (Alarm clock)
# 15 -> SIGTERM (Termination signal)
trap "printf 'Extraction operation interrupted, exiting\n'; rm -fr '$work_dir'; exit 1;" 1 2 3 4 6 9 11 13 14 15

if test "$silent" = "false";
then
    printf "Extracting installer ...\n"
fi

#extract, uncompress, and untar P2 installer
archive_start_line_number=$(awk '/^__BEGIN_TAR__/ {print NR + 1; exit 0; }' "$this_script")

# Extract payload tar file
tail -n+$archive_start_line_number "$this_script" | tar x -C "$work_dir"

installer_dir="$work_dir/installer"
mkdir -p "$installer_dir"
tar -xzf "$work_dir/$INSTALLER_TAR_FILE" -C "$installer_dir"
rm -f "$work_dir/$INSTALLER_TAR_FILE"
tar -xf "$work_dir/$REPO_TAR_FILE" -C "$installer_dir"
rm -f "$work_dir/$REPO_TAR_FILE"

#@POSTPROCESS@

# Check to see if the JRE can be found
if test ! -d "$installer_dir/jre/bin";
then
    printf "JRE not found in $installer_dir/jre/bin\n" >> ${log_output}
    if test "$silent" = "false";
    then
        printf "JRE not found in $installer_dir/jre/bin\n"
    fi
    exit 1
fi

if test "$extract_only" = "false";
then
    if test "$silent" = "false";
    then
        echo "Starting installer ..."
    fi

    printf "\nCommand: ${installer_dir}/setup -vm ${installer_dir}/jre/bin ${args} -install.once -install.data=${base_log_dir} -data ${timestamp_log_dir} -vmargs -XX:ErrorFile=${timestamp_log_dir}/jre_error.log" >> ${log_output}

    if test "$suppress_errors" = "false";
    then
        "${installer_dir}"/setup -vm "${installer_dir}"/jre/bin ${args} -install.once -install.data="${base_log_dir}" -data "${timestamp_log_dir}" -vmargs -XX:ErrorFile="${timestamp_log_dir}"/jre_error.log
    else
        "${installer_dir}"/setup -vm "${installer_dir}"/jre/bin ${args} -install.once -install.data="${base_log_dir}" -data "${timestamp_log_dir}" -vmargs -XX:ErrorFile="${timestamp_log_dir}"/jre_error.log 2> /dev/null
    fi
else
    printf "Installer extracted to $work_dir\n" >> "${log_output}"
    printf "Use $work_dir/installer/setup to start the installer using the following command:\n" >> "${log_output}"
    printf "$installer_dir/setup -vm $installer_dir/jre/bin [options]\n" >> "${log_output}"
    if test "$silent" = "false";
    then
        printf "Installer extracted to $work_dir\n"
        printf "Use $work_dir/installer/setup to start the installer using the following command:\n"
        printf "$installer_dir/setup -vm $installer_dir/jre/bin [options]\n"
    fi
fi

exit 0

### end of script
__BEGIN_TAR__
