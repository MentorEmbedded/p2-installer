#! /usr/bin/env bash

# build_linux_p2_sfx.sh - Default entry point to build linux
# p2 installer extraction application

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

path_to_internal_script=$(dirname $(readlink -e "$0"))/internal

. "$path_to_internal_script"/core_build_linux_p2_sfx.sh

# This function is an overridden function from 
# core_build_linux_p2_sfx.sh. It can be used to replace the 
# #@PREPROCESS@ and #@POSTPROCESS@ tags in the p2_sfx.sh script
# This function has access to all of the variables in the 
# core_build_linux_p2_sfx.sh
p2_sfx_linux_sfx_script_process()
{
# The following commented out line is an example of what could be
# be done.
#    sed -f "$path_to_somesedscript "$sfx_script" > "$sfx_script"

    : # null statement

}

# The following function allows the user to perform post processing 
# of the bin file
p2_sfx_linux_sfx_script_postprocess()
{
    : # null statement
}

p2_sfx_linux_main $@ --script-dir "$path_to_internal_script"
