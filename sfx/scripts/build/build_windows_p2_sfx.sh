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

# build_windows_p2_sfx.sh - Default entry point to build windows
# p2 installer extraction application

p2_sfx_windows_path_to_internal_script=$(dirname $(readlink -e "$0"))/internal

. "$p2_sfx_windows_path_to_internal_script"/core_build_windows_p2_sfx.sh

# Change the value of this to point at a splash screen bitmap. 
# It must be a 24 bit BMP
p2_sfx_windows_path_to_bmp="$p2_sfx_windows_path_to_internal_script"/../../../resources/windows/splash.bmp

# Change the following value to your organization's name
p2_sfx_windows_company_name="My Company Name"

# This function may be used to sign all of windows exe's that are 
# internal to the p2 installer. This typically does not include
# the self extracting exe itself.
p2_sfx_windows_sign_internal_exes()
{
#    for exefile in $(find "$installer" -iname '*.exe'); do
#        tmpfile="$scratch"/$(basename "$exefile")
#        cp "$exefile" "$tmpfile"
#        rm -f "$exefile"
#        #### sign tmpfile $exefile is the output
#        chmod a+x "$exefile"
#        rm -f "$tmpfile"
#    done
    : #null command
}

# p2_sfx_windows_append_marker_string - Default implementation. Can be 
# used to append a marker string to the output exe before it is signed
p2_sfx_windows_append_marker_string()
{
    : # null command
}

# This function may be used to digitally sign the self extraction
# executable.
p2_sfx_windows_sign_output_file()
{
    # sign the file
    # osslsigncode -pkcs12 "$some_certificate_file -pass your_sign_password -i http://timestamp.verisign.com/scripts/timstamp.dll -in "$tmp_output" -out "$output"
    # Remove the following line if signing the output file
    cp "$tmp_output" "$output"
}


# This function can be used to perform additional processing on a 
# particular $repo. 
p2_sfx_windows_repo_process()
{
    : #null command
}

# p2_sfx_windows_create_splash_bmp - Create a source file containing the 
# splash bitmap data. If you wish to change the default splash screen 
# image used by the extraction application, modify the value of 
# path_to_p2_sfx_windows_sfx_bmp to point at the BMP format image you wish
# to use. Note that it must be a 24 bit BMP image format.
p2_sfx_windows_create_splash_bmp()
{
    /usr/bin/xxd -i "$p2_sfx_windows_path_to_bmp" > "$splash_file"
    sed -i "s|unsigned char .*\[\]|unsigned char splash_bmp\[\]|g" "$splash_file"
}

# p2_sfx_windows_create_manifest - Create a manifest file which is linked to 
# the application. This function assumes output, scratch, rc_file, and
# CLEANUP_LIST variables are assigned to valid values.
p2_sfx_windows_create_manifest()
{
    local manifest_file
    local year
    local company_name
    local of_basename_w_ext
    local of_basename_wo_ext

    # see the following URL for more information
    # http://msdn.microsoft.com/en-us/library/windows/desktop/aa380599(v=vs.85).aspx
    manifest_file=`basename "${output}".manifest`
    year=$(date +"%Y")
    cp "$p2_sfx_windows_path_to_internal_script"/../../../resources/windows/installer.manifest "$scratch"/"$manifest_file"
    CLEANUP_LIST="$CLEANUP_LIST $scratch/$manifest_file"
    of_basename_w_ext="$(basename "$output")"
    of_basename_wo_ext="${of_basename_w_ext%.*}"

    ### Replace the following string with your company's name
    company_name="$p2_sfx_windows_company_name"

    cat > "$rc_file" <<EOF
#define RT_MANIFEST 24
#define APP_MANIFEST 1

APP_MANIFEST RT_MANIFEST "$scratch/$manifest_file"
the_icon ICON "$p2_sfx_windows_path_to_internal_script/../../../resources/windows/setup.ico"
1 VERSIONINFO
FILEVERSION     1,0,0,0
PRODUCTVERSION  1,0,0,0
BEGIN
  BLOCK "StringFileInfo"
  BEGIN
    BLOCK "040904E4"
    BEGIN
      VALUE "CompanyName", "$company_name"
      VALUE "FileDescription", "Self extraction program for P2 installer"
      VALUE "FileVersion", "1.0.0.0"
      VALUE "InternalName", "$of_basename_wo_ext"
      VALUE "LegalCopyright", "Copyright (C) $year, $company_name"
      VALUE "OriginalFilename", "$of_basename_w_ext"
      VALUE "ProductName", "$company_name Self Extraction Application"
      VALUE "ProductVersion", "1.0.0.0"
    END
  END

END
EOF
}

p2_sfx_windows_main $@ --script-dir "$p2_sfx_windows_path_to_internal_script"
