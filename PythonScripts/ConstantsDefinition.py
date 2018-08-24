#!/usr/bin/env python

import os

# navigate to 'CI_tool' that is the base directory 
base_dir_path_norm =  os.path.dirname(os.path.abspath(__file__))
base_dir_path_norm = os.path.normpath(base_dir_path_norm)
base_dir_path_norm = base_dir_path_norm.split(os.sep)
base_dir_path = ''
base_dir_path_diff = ''
for i in range(0, len(base_dir_path_norm)):
	if (i < 3):
		base_dir_path = base_dir_path + base_dir_path_norm[i] + '\\'
		base_dir_path_diff = base_dir_path_diff + base_dir_path_norm[i] + '\\'
	if (i==3): 
		base_dir_path = base_dir_path + base_dir_path_norm[i] + '\\'
		break

# directories definition
dir_repository_older_commit = base_dir_path + r'CI_tool_diff_older_commit\master'
dir_repository_newer_commit = base_dir_path + r'CI_tool_diff_newer_commit\master'
dir_repository_master = base_dir_path + r'master'
dir_repository_master_sourceCode = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_older_commit_sourceCode = dir_repository_older_commit + r'\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_newer_commit_sourceCode = dir_repository_newer_commit + r'\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_master_unitTests = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\test\java'
dir_repository_master_integrationTests = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\integration-test\java'
dir_sourceCode_diff = base_dir_path_diff + r'CI_tool_diff_repo\master\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_default_pom_file = base_dir_path + r'master\JavaWorkspace_MT\UnitUnderTest\pom.xml'
dir_repository_updated_pom_file = base_dir_path + r'master\JavaWorkspace_MT\UnitUnderTest\updated_pom.xml'

# All Files common
elem_start = "/********"
elem_stop =  "********/"
elem_skip_header_1 = "* Auxiliary piece of code"
elem_skip_header_2 = "Class Attributes"
elem_exceptions_thrown = "Exceptions thrown"
elem_exceptions_handled = "Exceptions handled"

# Source Code Files specific
elem_ok_header_code = "Method Name"
elem_affected_internal_variables = "Affected internal variables"
elem_affected_external_variables = "Affected external variables"
elem_called_internal_functions = "Called internal functions"
elem_called_external_functions = "Called external functions"

# Unit Tests & integration Test files
elem_ok_header_test = "Test Name"
elem_req_TBV = "Requirements TBV"

# Unit Tests files specific
elem_internal_variables_TBV = "Internal variables TBV"
elem_external_variables_TBV = "External variables TBV"
elem_mocked_external_methods = "Mocked external methods"
elem_exceptions_thrown_TBV = "Exceptions thrown TBV"
