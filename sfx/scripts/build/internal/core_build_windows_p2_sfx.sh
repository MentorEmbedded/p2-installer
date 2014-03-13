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
# core_build_windows_p2_sfx.sh
#
# Contents:
# Create a self extracting binary for Windows hosts
#
# This script will create a self extracting binary image for the p2
# installer to be installed on Windows. It uses the incbin assembly 
# directive to link the binary data from the intaller as binary blobs
# to the self extraction program. 
# 
# The zip utlity is used to create the binary blobs and unzip is 
# used on the end user machine to extract the binary blobs on the
# user file system. Unzip itself is bundled as a binary blob with 
# the extractor so as not to require the end user to install any
# additional software.
#
# The unzip utility is contained in sfx/resources/windows/unzip.exe
# The source code used to build unzip.exe is found in the same 
# directory in unzip60.tar.gz. The license for unzip can be found 
# in the zip file and also here:
#
# http://www.info-zip.org/pub/infozip/license.html
#
# Find the variable "company_name" below and change the value to the
# name of your organization.
#
# In theory any MW Windows toolchain which includes incbin can be 
# used, but only mingw32 has been tested.
#
#
# Script Arguments:
# --installer-dir directory where the installer to be packaged by sfx 
#                 can be found. Required.
# --scratch-dir   directory where temporary objects and files can be 
#                 written during sfx generation. If this argument is
#                 omitted, it will use the current directory. Optional.
# --output-file   path and file name of sfx. Required.
# --compiler      path to windows compiler. Required.
# --resource-compiler path to windows resource compiler. Required.
# --script-dir    path to the directory which contains this script. 
#                 Required
# --silent        turn off all messages from this script. Optional.
#######################################################################

# All errors are fatal
set -e

# The following variables must matcth the cooresponding values 
# in p2_sfx.h
p2_sfx_unix_base_path=@UNIX_BASE_PATH@ #define UNIX_BASE_PATH
p2_sfx_windows_base_path=@WINDOWS_BASE_PATH@ #define WINDOWS_BASE_PATH
p2_sfx_setup=@SETUP@ #define SETUP


# p2_sfx_windows_global_statement - This function adds the global variable 
# information to the generated assembly and c files. The global varaibles
# are the symbols used to reference the included binary files (one variable
# at the start of the file and another and the end).
p2_sfx_windows_global_statement()
{
    local var_name

    var_name="$1"

    echo "extern unsigned char _${var_name}_start, _${var_name}_end;" >> "$GENERATED_C_FILE"
    echo ".global __${var_name}_start, __${var_name}_end;" >> "$GENERATED_ASM_FILE"
}

# p2_sfx_windows_incbin_statement- adds an incbin statement to the generated 
# assembly file as well as the reference to the global symbols necessary to 
# reference the binary included by the incbin statement.
#
# An example of an incbin statement in an assembly file is as follows:
#
# .p2_align 3
# __the_file_start_symbol:
# .incbin "absolute path to file"
# __the_file_end_symbol:
#
# The first byte of each binary file included by incbin is pointed at 
# by __the_file_start_symbol. The byte size of the file is calculated 
# by subtracting __the_file_start_symbol from __the_file_end_symbol. i
# The path and name of the file are not preserved by the incbin statement.
#
p2_sfx_windows_incbin_statement()
{
    local var_name
    local bin_file

    var_name="$1"
    bin_file="$2"

    echo "        &_${var_name}_start," >> "$GENERATED_C_FILE"
    echo "        &_${var_name}_end," >> "$GENERATED_C_FILE"

    echo ".p2align 3" >> "$GENERATED_ASM_FILE"
    echo "__${var_name}_start:" >> "$GENERATED_ASM_FILE"
    echo ".incbin \"${bin_file}\"" >> "$GENERATED_ASM_FILE"
    echo "__${var_name}_end:" >> "$GENERATED_ASM_FILE"
}

# p2_sfx_windows_sign_internal_exes - This stub is  provides a hook to 
# digitally sign the exe's that are internal to the p2 installer.
# This routine is typically overridden by other scripts
p2_sfx_windows_sign_internal_exes()
{
    : #null command
}

# p2_sfx_windows_add_installer_definitions - Add the installer related declaration 
# statements for the global variables for the variables  created by the incbin 
# statements for the installer. Also start the array of file descriptors.
p2_sfx_windows_add_installer_definitions()
{
    INSTALLER_FILE_DESCS="installerFileDescs"
    INSTALLER_NUM_FILE_DESCS="1"
    INSTALLER_ZIP_FILE="installer.zip"
    local installer_sym="installer"

    pushd "$installer"/.. > /dev/null
    p2_sfx_windows_sign_internal_exes
    zip -r "$scratch"/$(basename "$INSTALLER_ZIP_FILE") $(basename "$installer") -x $(basename "${installer}")/repos/* -q
    CLEANUP_LIST="$CLEANUP_LIST $scratch/$INSTALLER_ZIP_FILE"
    popd > /dev/null

    p2_sfx_windows_global_statement "$installer_sym" 

    echo "P2_SFX_FILE_DESC ${INSTALLER_FILE_DESCS}[] =" >> "$GENERATED_C_FILE"
    echo "{" >> "$GENERATED_C_FILE"
    echo "    {" >> "$GENERATED_C_FILE"
    echo "        \"${INSTALLER_ZIP_FILE}\"," >> "$GENERATED_C_FILE"
    p2_sfx_windows_incbin_statement $installer_sym "${scratch}"/"${INSTALLER_ZIP_FILE}"
    echo "    }" >> "$GENERATED_C_FILE"
    echo "};" >> "$GENERATED_C_FILE"
    echo "" >> "$GENERATED_C_FILE"
}

# p2_sfx_windows_add_installer_bundle - Add the bundle for the core installer
p2_sfx_windows_add_installer_bundle()
{
    echo "    {" >> "$GENERATED_C_FILE"
    echo "        $INSTALLER_FILE_DESCS," >> "$GENERATED_C_FILE"
    echo "        $INSTALLER_NUM_FILE_DESCS," >> "$GENERATED_C_FILE"
    echo "        \"\"," >> "$GENERATED_C_FILE"
    echo "        0," >> "$GENERATED_C_FILE"
    echo "        0" >> "$GENERATED_C_FILE"
    echo "    }," >> "$GENERATED_C_FILE"
}

# p2_sfx_windows_add_unzip_definitions - add all of the declarations necessary
# for the unzip uility. Also add a command to unzip the core installer zip
# file.

# The "$p2_sfx_unix_base_path" string used below represents the base installation directory on
# the machine where SCB will be installed where the directory separators are
# unix style forward slashes. The "$p2_sfx_windows_base_path" string represents the same base 
# installation directory only with windows style backward slashes for 
# directory separators. 
p2_sfx_windows_add_unzip_definitions()
{
    UNZIP_FILE_DESCS="unzipFileDescs"
    UNZIP_NUM_FILE_DESCS="1"
    UNZIP_COMMANDS="unzipCommands"
    UNZIP_NUM_COMMANDS="2"

    local unzip_sym="unzip"

    p2_sfx_windows_global_statement "$unzip_sym" 

    echo "P2_SFX_FILE_DESC ${UNZIP_FILE_DESCS}[] =" >> "$GENERATED_C_FILE"
    echo "{" >> "$GENERATED_C_FILE"
    echo "    {" >> "$GENERATED_C_FILE"
    echo "        \"unzip.exe\"," >> "$GENERATED_C_FILE"
    p2_sfx_windows_incbin_statement "$unzip_sym" "$script_dir"/../../../resources/windows/unzip.exe
    echo "    }" >> "$GENERATED_C_FILE"
    echo "};" >> "$GENERATED_C_FILE"
    echo "" >> "$GENERATED_C_FILE"
    echo "char * ${UNZIP_COMMANDS}[] =" >> "$GENERATED_C_FILE"
    echo "{" >> "$GENERATED_C_FILE"
    echo "    \"\\\"${p2_sfx_windows_base_path}unzip.exe\\\" -q \\\"${p2_sfx_unix_base_path}${INSTALLER_ZIP_FILE}\\\" -d \\\"${p2_sfx_unix_base_path}\\\"\", \"cmd /C del \\\"${p2_sfx_windows_base_path}${INSTALLER_ZIP_FILE}\\\"\"" >> "$GENERATED_C_FILE"
    echo "};" >> "$GENERATED_C_FILE"
}

# p2_sfx_windows_add_unzip_bundle - add the bundle for the unzip utility
p2_sfx_windows_add_unzip_bundle()
{
    echo "    {" >> "$GENERATED_C_FILE"
    echo "        $UNZIP_FILE_DESCS," >> "$GENERATED_C_FILE"
    echo "        $UNZIP_NUM_FILE_DESCS," >> "$GENERATED_C_FILE"
    echo "        \"\"," >> "$GENERATED_C_FILE"
    echo "        $UNZIP_COMMANDS," >> "$GENERATED_C_FILE"
    echo "        $UNZIP_NUM_COMMANDS" >> "$GENERATED_C_FILE"
    echo "    }," >> "$GENERATED_C_FILE"
}

# p2_sfx_windows_process_repo - This stub 
# provides a mechanism to perform additional 
# processing on the repos
p2_sfx_windows_repo_process()
{
    : #null command
}

# p2_sfx_windows_add_repo_definitions - add all of the repository declarations
# necessary to add the repository bundle to the generated c file. This
# function will search for and include all of the repos found in the repos
# subdirectory of the installer
p2_sfx_windows_add_repo_definitions()
{
    REPO_FILE_DESCS="repoFileDescs"
    REPO_NUM_FILE_DESCS=
    REPO_COMMANDS="repoCommands"
    REPO_NUM_COMMANDS=2

    local repo_list
    local count
    local basename_repos_list
    local repos_list
    local repo
    local repo_cmd_list
    local num_repos
    local file_name
    local vm_arg

    repo_cmd_list=
    repo_list=

    basename_repos_list=$(ls -1 "$installer"/repos)
    num_repos=$(echo "$basename_repos_list" | wc -w)

    pushd "$installer"/repos > /dev/null
    count=0
    for repo in $basename_repos_list; do
        if [ -d "$installer"/repos/$repo ]; then
            zip -r "$scratch"/repo_$repo $repo -q
            CLEANUP_LIST="$CLEANUP_LIST $scratch/repo_$repo.zip"
            repos_list="$repos_list "$scratch"/repo_$repo.zip"
            repo_cmd_list="\"\\\"${p2_sfx_windows_base_path}unzip.exe\\\" -q \\\"${p2_sfx_unix_base_path}$(basename ${installer})/repos/repo_${repo}.zip\\\" -d \\\"${p2_sfx_unix_base_path}$(basename ${installer})/repos\\\"\", \"cmd /C del \\\"${p2_sfx_windows_base_path}$(basename ${installer})\\\\repos\\\\repo_${repo}.zip\\\"\", $repo_cmd_list"
            REPO_NUM_COMMANDS=$(expr $REPO_NUM_COMMANDS + "2")
        else
            p2_sfx_windows_repo_process "$repo" "$repo_cmd_list"
            repos_list="$repos_list "$installer"/repos/$repo"
        fi
        p2_sfx_windows_global_statement repo_$count 
        count=$(expr $count + "1")
    done
    popd > /dev/null

    echo "P2_SFX_FILE_DESC ${REPO_FILE_DESCS}[] =" >> "$GENERATED_C_FILE"
    echo "{" >> "$GENERATED_C_FILE"
    count=0
    for repo in $repos_list; do
        echo "     {" >> "$GENERATED_C_FILE"
        echo "         \"$(basename $repo)\"," >> "$GENERATED_C_FILE"
        p2_sfx_windows_incbin_statement repo_$count "$repo"
        echo "     }," >> "$GENERATED_C_FILE"
        echo "" >> "$GENERATED_C_FILE"
        count=$(expr $count + "1")
    done
    echo "};" >> "$GENERATED_C_FILE"
    echo "" >> "$GENERATED_C_FILE"
    REPO_NUM_FILE_DESCS=$count

    echo "char * ${REPO_COMMANDS}[] =" >> "$GENERATED_C_FILE"
    echo "{" >> "$GENERATED_C_FILE"
    echo "    ${repo_cmd_list}" >> "$GENERATED_C_FILE"

    vm_arg=" -vm \\\"${p2_sfx_unix_base_path}$(basename ${installer})/jre/bin\\\""
    echo "    \"cmd /C del \\\"${p2_sfx_windows_base_path}unzip.exe\\\"\", \"\\\"${p2_sfx_windows_base_path}$(basename ${installer})\\\\${p2_sfx_setup}\\\"${vm_arg}\"" >> "$GENERATED_C_FILE"
    echo "};" >> "$GENERATED_C_FILE"
}

# p2_sfx_windows_add_repo_bundle - Add the repository bundle to the 
# generated c file
p2_sfx_windows_add_repo_bundle()
{
    local repo_dir

    repo_dir="$(basename "$installer")\\\\repos\\\\"

    echo "    {" >> "$GENERATED_C_FILE"
    echo "        $REPO_FILE_DESCS," >> "$GENERATED_C_FILE"
    echo "        $REPO_NUM_FILE_DESCS," >> "$GENERATED_C_FILE"
    echo "        \"${repo_dir}\"," >> "$GENERATED_C_FILE"
    echo "        $REPO_COMMANDS," >> "$GENERATED_C_FILE"
    echo "        $REPO_NUM_COMMANDS" >> "$GENERATED_C_FILE"
    echo "    }," >> "$GENERATED_C_FILE"
}

# p2_sfx_windows_create_splash_bmp - creates a C file
# containing the binary data from a 24 bit BMP file
p2_sfx_windows_create_splash_bmp()
{
    echo "p2_sfx_windows_create_splash_bmp function must produce a file which can be compiled and assigned to the variable splash_file"
    exit 1
}

# p2_sfx_windows_create_manifest - Default implementation stub of function 
# which is responsible for creating a manifest file which will be linked to 
# the output exe.
p2_sfx_windows_create_manifest()
{
    echo "p2_sfx_windows_create_manifest function must produce a Windows manifest file which can be compiled with the windows resource file and linked to the output exe."
    exit 1
}

# p2_sfx_windows_sign_output_file - Default implementation. Can be replaced 
# with code to sign the output file.
p2_sfx_windows_sign_output_file()
{
    cp "$tmp_output" "$output"
}

p2_sfx_windows_build()
{
    local installer
    local output
    local rc_file
    local splash_file
    local core_c_file
    local core_c_include_dir
    local core_c_object
    local compiler_for_host
    local windres_compiler
    local GENERATED_ASM_FILE
    local GENERATED_C_FILE
    local INSTALLER_FILE_DESCS
    local INSTALLER_NUM_FILE_DESCS
    local INSTALLER_ZIP_FILE
    local UNZIP_FILE_DESCS
    local UNZIP_NUM_FILE_DESCS
    local UNZIP_COMMANDS
    local UNZIP_NUM_COMMANDS
    local REPO_FILE_DESCS
    local REPO_NUM_FILE_DESCS
    local REPO_COMMANDS
    local REPO_NUM_COMMANDS
    local CLEANUP_LIST

    # get inputs
    installer="$1"
    scratch="$2"
    output="$3"
    compiler_for_host="$4"
    windres_compiler="$5"

    GENERATED_ASM_FILE="$scratch"/p2_sfx_binary_data.s
    GENERATED_C_FILE="$scratch"/p2_sfx_file_bundles.c
    rc_file="$scratch"/p2_sfx_windows.rc
    splash_file="$scratch"/p2_sfx_splash_bmp.c
    core_c_file="$script_dir"/../../../src/windows/p2_sfx.c
    core_c_include_dir="$script_dir"/../../../src/windows
    core_c_object="$scratch"/p2_sfx.o
    CLEANUP_LIST="$CLEANUP_LIST $GENERATED_ASM_FILE $GENERATED_C_FILE $rc_file $splash_file $core_c_object"

    # Begin the creation of the generated files
    echo "#include \"p2_sfx.h\"" > "$GENERATED_C_FILE"
    echo ".section .rodata" > "$GENERATED_ASM_FILE"
    echo "" >> "$GENERATED_C_FILE"

    p2_sfx_windows_add_installer_definitions 
    p2_sfx_windows_add_unzip_definitions 
    p2_sfx_windows_add_repo_definitions 

    echo "int P2_SFX_NUM_BUNDLES=3;" >> "$GENERATED_C_FILE"
    echo "" >> "$GENERATED_C_FILE"
    echo "P2_SFX_FILE_BUNDLE bundles[] =" >> "$GENERATED_C_FILE"
    echo "{" >> "$GENERATED_C_FILE"
    p2_sfx_windows_add_installer_bundle 
    p2_sfx_windows_add_unzip_bundle
    p2_sfx_windows_add_repo_bundle 
    echo "};" >> "$GENERATED_C_FILE"

    chmod a+r "$GENERATED_C_FILE"
    chmod a+r "$GENERATED_ASM_FILE"

    # Create source file for splash bitmap
    p2_sfx_windows_create_splash_bmp

    $compiler_for_host -c "$GENERATED_ASM_FILE" -o ${GENERATED_ASM_FILE%.*}.o
    $compiler_for_host -c "$GENERATED_C_FILE"  -I"$script_dir"/../../../src/windows -o ${GENERATED_C_FILE%.*}.o
    $compiler_for_host -c "$core_c_file"  -I"$core_c_include_dir" -o "$core_c_object"
    $compiler_for_host -c "$splash_file" -o ${splash_file%.*}.o

    CLEANUP_LIST="$CLEANUP_LIST ${GENERATED_ASM_FILE%.*}.o ${GENERATED_C_FILE%.*}.o ${splash_file%.*}.o"

    #Create_manifest_file
    p2_sfx_windows_create_manifest

    # Compile resource file
    $windres_compiler "$rc_file" -O coff -o ${rc_file%.*}.res
    CLEANUP_LIST="$CLEANUP_LIST ${rc_file%.*}.res"
    
    # Link everything together
    tmp_output="$scratch"/tmp.exe
    $compiler_for_host -mwindows -o "$tmp_output" /${GENERATED_C_FILE%.*}.o "$core_c_object" ${GENERATED_ASM_FILE%.*}.o  ${splash_file%.*}.o ${rc_file%.*}.res
    p2_sfx_windows_sign_output_file
    rm -f "$tmp_output"
    chmod a+x $output
    for item in "$CLEANUP_LIST"; do
#        rm -f ${item}
:
    done
}

p2_sfx_windows_usage() {
  echo "$0 [arguments]"
  echo "The following arguments are supported:"
  echo "--installer-dir [directory where the installer to be packaged by sfx can be found; required]"
  echo "--scratch-dir [directory where temporary objects and files can be written during sfx generation; optional. Uses current directory if not specified]"
  echo "--output-file [path and file name of generated sfx; required]"
  echo "--compiler [path to windows compiler; required]"
  echo "--resource-compiler [path to windows resource compiler; required]"
  echo "--silent [Turn off messages from this script; optional]"
}

p2_sfx_windows_main()
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
        --compiler)
          shift
          compiler="$1"
          ;;
        --resource-compiler)
          shift
          resource_compiler="$1"
          ;;
       --script-dir)
         shift
         script_dir="$1";
         ;;
        --silent)
          shift
          silent="true";
          ;;
        *)
          echo "Unknown argument $1"
          p2_sfx_windows_usage "$0"
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
      p2_sfx_windows_usage $0
      exit 1
    fi

    if test -z "script_dir"
    then
        echo "The core_build_windows_p2_sfx.sh script also requires the following argument"
        echo "--script-dir [path to the directory containing this script]"
    fi

    if test -z "$silent"
    then
        echo "Installer to be used: $installer_dir"
        echo "Scratch directory to be used: $scratch_dir"
        echo "Output file to be created: $output_file"
        echo ""
    fi
    p2_sfx_windows_build "$installer_dir" \
                         "$scratch_dir" \
                         "$output_file" \
                         "$compiler" \
                         "$resource_compiler" \
                         "$script_dir"
}
