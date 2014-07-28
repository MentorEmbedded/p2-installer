#! /usr/bin/env bash

#######################################################################
#  Copyright (c) 2014 Mentor Graphics and others.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License 
#  v1.0 which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
#  Contributors:
#    Mentor Graphics - initial API and implementation
#######################################################################

#######################################################################
#
#  core_build_linux_p2_sfx.sh
#
#  Create a self extracting script for the p2 installer for Linux 
#  hosts. 
#
#  The basic idea is to pull everything required by the installer 
#  into a single payload file (a single tar file). This idea is 
#  described in an Linux Journel article written by Jeff Parent 
#
#  http://www.linuxjournal.com/node/1005818
#
#  First the repositories (repos) are put into their own uncompressed 
#  tar file. Next, the p2 installer are put into its own compressed
#  tar file. 
#
#  The p2 installer itself (ie. the RCP) is compressed but the 
#  repos are not recompressed because they are already compressed.
#  For performance reasons they shouldn't be recompressed again.
#
#  Last, both of those tar files are put into a tar file which is 
#  appended to the p2_sfx.sh script, which knows how to extract the 
#  payload and the 2 tar files, and start the installer on the users 
#  host.
#
#  Once the final payload file has been created, it is appended to 
#  a script which knows how to extract the payload and starts the 
#  installer the users host.
#
# Note that the extraction script expects to find a JRE binary
# in the $installer_dir/jre/bin directory
#
# Arguments:
# --installer-dir directory where the installer to be packaged by sfx 
#                 can be found. Required.
# --scratch-dir   directory where temporary objects and files can be 
#                 written during sfx generation. If this argument is
#                 omitted, it will use the current directory. Optional.
# --output-file   path and file name of sfx. Required.
# --script-dir    path to the directory which contains this script. Required.
# --silent        turn off all messages from this script. Optional.
#######################################################################

# All errors are fatal
set -e

# The following function is typically overriden by users of this script
p2_sfx_linux_sfx_script_process()
{
    : # null statement
}

p2_sfx_linux_build()
{
    local installer
    local scratch
    local output
    local payload_repo_tar_file
    local payload_installer_zip_file
    local payload_uncmp_installer_tar_file
    local payload_tar_file
    local sfx_script_prefix
    local sfx_script
    local installer_needed_bytes

    installer="$1"
    scratch="$2"
    output="$3"
    data_dir="$5"

    payload_repo_tar_file="$scratch"/repo.tar
    payload_installer_zip_file="$scratch"/installer.tgz
    payload_uncmp_installer_tar_file="$scratch"/uncmp_installer.tar
    payload_tar_file="$scratch"/payload.tar

    sfx_script_prefix="$scratch"/sfx_script_prefix.sh
    sfx_script=$(readlink -e "$script_dir/../../src/linux/p2_sfx.sh")
    cp "$sfx_script" "$scratch/p2_sfx.sh"
    sfx_script="$scratch/p2_sfx.sh"
    chmod a+rw $sfx_script

    if test -z "$silent"
    then
        echo "Creating ..."
    fi

    pushd "$installer" > /dev/null

    # Check to see if the repos directory is empty; if it is, create
    # a temporary file (the sfx expects to find a repos tarfile)
    if [[ -z $(ls repos) ]]; then
        if test -z "$silent"
        then
            echo "Error: repositories not found"
        fi
        exit 1
    fi

    # Create but don't compress tar file containing the repos
    tar -cf "$payload_repo_tar_file" repos

    installer_needed_bytes=$(stat -c %s "$payload_repo_tar_file")
    # Multiply by two because it will be untar'd on the user's host
    installer_needed_bytes=$(expr $installer_needed_bytes \* "2")

    if test -z "$silent"
    then
        echo "Size of repositories: $(stat -c %s "$payload_repo_tar_file")"
    fi

    # Create a compressed tar file of the installer
    # Exclude the repos

    tar -czf "$payload_installer_zip_file" ./ --exclude=repos
    installer_needed_bytes=$(expr $installer_needed_bytes + $(stat -c %s "$payload_installer_zip_file"))
    if test -z "$silent"
    then
        echo "Compressed p2 installer size: $(stat -c %s "$payload_installer_zip_file")"
    fi    

    # Create an uncompressed installer tar for size calculation only
    # It will not be included in the payload.
    tar -cf "$payload_uncmp_installer_tar_file" ./ --exclude=repos
    installer_needed_bytes=$(expr $installer_needed_bytes + $(stat -c %s "$payload_uncmp_installer_tar_file"))
    if test -z "$silent"
    then
        echo "Uncompressed p2 installer size: $(stat -c %s "$payload_uncmp_installer_tar_file")"
    fi    

    # add a bit extra to account for tar file overhead; An extra 10k is 
    # just a swag
    installer_needed_bytes=$(expr $installer_needed_bytes + "10240")

    popd > /dev/null

    pushd "$scratch" > /dev/null

    # Create final payload tar file
    tar -cf "$payload_tar_file" $(basename "$payload_installer_zip_file") $(basename "$payload_repo_tar_file")

    # Create the prefix script 
    cat > $sfx_script_prefix <<EOF
#!/bin/sh
# This number provides a rough estimate and isn't intended to be exact.
ARCHIVE_BYTES=$installer_needed_bytes
INSTALLER_TAR_FILE=$(basename "$payload_installer_zip_file")
REPO_TAR_FILE=$(basename "$payload_repo_tar_file")
DATA_DIR=$data_dir
EOF
    chmod a+r $sfx_script_prefix

    # Function override point
    p2_sfx_linux_sfx_script_process

    cat "$sfx_script_prefix" "$sfx_script" "$payload_tar_file" > "$output"
    chmod a+x "$output"

    popd > /dev/null

    if test -z "$silent"
    then
        echo "Finished"
    fi

    if test "$scratch_dir_created" = "true"
    then
        rm -fr "$scratch"
    else
        rm "$payload_repo_tar_file"
        rm "$payload_installer_zip_file"
        rm "$payload_uncmp_installer_tar_file"
        rm "$payload_tar_file"
        rm "$sfx_script_prefix"
        rm "$sfx_script"
    fi
}

p2_sfx_linux_usage() {
  echo "$1 [arguments]"
  echo "The following arguments are supported:"
  echo "--installer-dir [directory where the installer to be packaged by sfx can be found; required]"
  echo "--scratch-dir [directory where temporary objects and files can be written during sfx generation; optional. Uses current directory if not specified]"
  echo "--data-dir [name of installer data directory; optional. Uses current default name if not specified (.p2_installer)]"
  echo "--output-file [path and file name of generated sfx; required]"
  echo "--silent [Turn off messages from this script; optional]"
}

p2_sfx_linux_main()
{
    while test "$#" -ge 1; do
        case "$1" in
        --installer-dir)
          shift
          installer_dir="$1";
          ;;
        --scratch-dir)
          shift
          scratch_dir="$1";
          ;;
        --output-file)
          shift
          output_file="$1";
          ;;
        --script-dir)
          shift
          script_dir="$1";
          ;;
        --data-dir)
          shift
          data_dir="$1";
          ;;
        --silent)
          shift
          silent="true";
          ;;
        *)
          echo "Unknown argument $1"
          p2_sfx_linux_usage "$0"
          exit 1
          ;;
       esac
       shift
    done

    if test -d "$installer_dir"
    then
        installer_dir=$(readlink -e "$installer_dir")
    elif test ! -z "$installer_dir" && test ! -d "$installer_dir"
    then
        installer_dir=""
    fi

    if test ! -z "$scratch_dir" && test ! -d "$scratch_dir"
    then
        mkdir -p "$scratch_dir"
        scratch_dir_created="true"
    elif test -z "$scratch_dir"
    then
        scratch_dir=`pwd`
    fi
    scratch_dir=$(readlink -e "$scratch_dir")

    if test ! -z "$output_file"
    then
        if test ! -d $(dirname "$output_file")
        then
            mkdir -p $(dirname "$output_file")
        fi
        output_file=$(readlink -e $(dirname "$output_file"))/$(basename "$output_file")
    fi

    if test ! -d "$script_dir"
    then
        script_dir=""
    fi

    if test -z "$installer_dir" \
       || test -z "$scratch_dir" \
       || test -z "$output_file"  
    then
      p2_sfx_linux_usage $0
      exit 1
    fi

    if test -z "script_dir"
    then
        echo "The  core_build_linux_p2_sfx.sh script also requires the following argument"
        echo "--script-dir [path to the directory containing this script]"
    fi

    if test -z "$silent"
    then
        echo "Installer to be used: $installer_dir"
        echo "Scratch directory to be used: $scratch_dir"
        echo "Output file to be created: $output_file"
        echo ""
    fi

    p2_sfx_linux_build "$installer_dir" \
                       "$scratch_dir" \
                       "$output_file"  \
                       "$script_dir"  \
                       "$data_dir"  \

    }
