#!/usr/bin/env python

# directories definition
dir_repository_older_commit = r'C:\Projects\Test_Integration_Jenkins\CI_tool\CI_tool_diff_older_commit\master'
dir_repository_newer_commit = r'C:\Projects\Test_Integration_Jenkins\CI_tool\CI_tool_diff_newer_commit\master'
dir_repository_master = r'C:\Projects\Test_Integration_Jenkins\CI_tool\master'
dir_repository_master_sourceCode = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_older_commit_sourceCode = dir_repository_newer_commit + r'\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_newer_commit_sourceCode = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\main\java'
dir_repository_master_unitTests = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\test\unitTests'
dir_repository_master_integrationTests = dir_repository_master + r'\JavaWorkspace_MT\UnitUnderTest\src\test\integrationTests'
dir_sourceCode_diff = r'C:\Projects\Test_Integration_Jenkins\CI_tool_diff_repo\master\JavaWorkspace_MT\UnitUnderTest\src\main\java'

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
