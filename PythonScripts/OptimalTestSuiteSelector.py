#!/usr/bin/env python

from __future__ import division
import git
import numpy as np
import time
from ConstantsDefinition import *
from AuxiliaryScripts import *
from OptimalTestSuiteSelector_AuxiliaryScripts import *

commit_newer_id = 0
commit_older_id = 1


print base_dir_path
# Set HEAD of Git repository to commits under analysis
commit_newer, commit_older = get_commit_from_repository(dir_repository_master, commit_newer_id, commit_older_id)	

print '1) Set HEAD of Git repository to commits under analysis \n'
print '\t commit_newer: ' + str(commit_newer)
print '\t commit_older: ' + str(commit_older)
print '\n'

# Generate git diff log between files modified or added in newer commit against older commit
diff_data_head_to_commit = build_git_diff_log_file(commit_newer, commit_newer_id, commit_older_id)

print '2) Generate git diff log between files in newer commit against older commit \n'
print '\t len(diff_data_head_to_commit): ' + str(len(diff_data_head_to_commit))
print '\n'

# Based on git diff log create a lists of modified files or added files
list_of_modified_files_to_be_processed, list_of_added_files_to_be_processed, list_of_all_changed_files = build_lists_of_files_to_be_processed(commit_newer, commit_newer_id, commit_older_id)

print '3) Based on git diff log create a lists of modified files or added files \n'
print '\t len(list_of_modified_files_to_be_processed): ' + str(len(list_of_modified_files_to_be_processed))
print '\t len(list_of_added_files_to_be_processed): ' + str(len(list_of_added_files_to_be_processed))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods with added lines
resulted_array_of_all_changes_in_file_blocks = build_resulted_array_of_changed_file_blocks(list_of_modified_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files)

print '4) Parse git diff log to separate file blocks (method body) for methods with added lines \n'
print '\t len(resulted_array_of_all_changes_in_file_blocks): ' + str(len(resulted_array_of_all_changes_in_file_blocks))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods with removed lines
resulted_array_of_removed_lines_in_changed_file_blocks = build_resulted_array_of_removed_lines(resulted_array_of_all_changes_in_file_blocks, list_of_modified_files_to_be_processed)

print '5) Parse git diff log to separate file blocks (method body) for methods with removed lines \n'
print '\t len(resulted_array_of_removed_lines_in_changed_file_blocks): ' + str(len(resulted_array_of_removed_lines_in_changed_file_blocks))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods with added lines
resulted_array_of_added_lines_in_changed_file_blocks = build_resulted_array_of_added_lines(resulted_array_of_all_changes_in_file_blocks, list_of_modified_files_to_be_processed)

print '5) Parse git diff log to separate file blocks (method body) for methods with added lines \n'
print '\t len(resulted_array_of_added_lines_in_changed_file_blocks): ' + str(len(resulted_array_of_added_lines_in_changed_file_blocks))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods that have been added
resulted_array_of_added_lines_in_added_file_blocks = build_resulted_array_of_added_file_blocks(list_of_added_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files)

print '6) Parse git diff log to separate file blocks (method body) for methods that have been added \n'
print '\t len(resulted_array_of_added_lines_in_added_file_blocks): ' + str(len(resulted_array_of_added_lines_in_added_file_blocks))
print '\n'

# Parse file blocks (method body) for methods with added lines to separate names of the methods, as an output the normalized path to the file that contains the methods is also returned
resulted_array_of_methods_with_added_lines_numbers = build_resulted_array_of_added_lines_numbers(resulted_array_of_added_lines_in_changed_file_blocks, list_of_modified_files_to_be_processed, commit_newer, dir_repository_newer_commit)

print '7) Parse file blocks (method body) for methods with added lines to separate names of the methods, as an output the normalized path to the file that contains the methods is also returned \n'
for methods_with_added_lines_numbers in resulted_array_of_methods_with_added_lines_numbers:
	print '\t normalized path of file and its methods with added lines:'  + str(methods_with_added_lines_numbers)
print '\n'

resulted_array_of_methods_with_removed_lines_numbers = []

# Parse file blocks (method body) for methods with removed lines to separate names of the methods, as an output the normalized path to the file that contains the methods is also returned
print '8) Parse file blocks (method body) for methods with removed lines to separate names of the methods, as an output the normalized path to the file that contains the methods is also returned \n'
if (len(resulted_array_of_removed_lines_in_changed_file_blocks) != 0):
	resulted_array_of_methods_with_removed_lines_numbers = build_resulted_array_of_removed_lines_numbers(resulted_array_of_removed_lines_in_changed_file_blocks, list_of_modified_files_to_be_processed, commit_older, dir_repository_older_commit)
	for methods_with_removed_lines_numbers in resulted_array_of_methods_with_removed_lines_numbers:
		print '\t normalized path of file and its methods with removed lines: ' + str(methods_with_removed_lines_numbers)
else:
	print '\t normalized path of file and its methods with removed lines does not contain any elments'
print '\n'

# Parse file blocks (method body) for methods that have been added to separate names of the methods, as an output the normalized path to the file that contains the methods is also returned	
resulted_array_of_methods_in_added_files = build_resulted_array_of_added_lines_numbers(resulted_array_of_added_lines_in_added_file_blocks, list_of_added_files_to_be_processed, commit_newer, dir_repository_newer_commit)

print '9) Parse file blocks (method body) for methods that have been added to separate names of the methods, as an output the normalized path to the file that contains the methods is also returned \n'
for methods_in_added_files in resulted_array_of_methods_in_added_files:
	print '\t normalized path of added file and its methods: ' + str(methods_in_added_files)
print '\n'

# Concatenate the results from points 7., 8. and 9. (i.e. build lists of all methods that have been affected with normalized path to the file that contains the methods)
resulted_array_of_affected_methods = []
resulted_array_of_aggregated_affected_methods = []

for normalized_file_and_its_methods_with_added_lines in resulted_array_of_methods_with_added_lines_numbers:
	resulted_array_of_affected_methods.append(normalized_file_and_its_methods_with_added_lines)


if (len(resulted_array_of_methods_with_removed_lines_numbers) != 0):
	resulted_array_of_aggregated_affected_methods = build_resulted_array_of_aggregated_affected_methods(resulted_array_of_methods_with_removed_lines_numbers, resulted_array_of_affected_methods)
if (len(resulted_array_of_methods_in_added_files) != 0):
	resulted_array_of_aggregated_affected_methods = build_resulted_array_of_aggregated_affected_methods(resulted_array_of_methods_in_added_files, resulted_array_of_affected_methods)

print '10) Concatenate the results from points 7., 8. and 9. (i.e. build single list of all methods that have been affected with normalized path to the file that contains the methods) \n'
for aggregated_affected_method in resulted_array_of_aggregated_affected_methods:
	print '\t normalized path of file and its aggregated affected methods: ' + str(aggregated_affected_method)
print '\n'	

# Create list of the methods affected by changes in newer commit against older commit 
resulted_list_of_affected_methods, resulted_list_of_normalized_files_of_changed_methods = build_resulted_list_of_changed_methods(resulted_array_of_affected_methods)

print '11) Create list of the methods affected by changes in newer commit against older commit \n'
for affected_method in resulted_list_of_affected_methods:
	print '\t affected method: ' + str(affected_method)
print '\n'
	
# Create list of the methods that call the methods affected by changes in newer commit against older commit 
resulted_list_of_methods_that_calls_changed_internal_method, resulted_list_of_methods_that_calls_changed_external_method = build_resulted_list_of_methods_that_calls_changed_method(resulted_array_of_affected_methods)

print '12) Create list of the methods that call the methods affected by changes in newer commit against older commit \n'
for method_that_calls_affected_internal_method in resulted_list_of_methods_that_calls_changed_internal_method:
	print '\t method that calls affected internal method: ' + str(method_that_calls_affected_internal_method)
	
for method_that_calls_affected_external_method in resulted_list_of_methods_that_calls_changed_external_method:
	print '\t method that calls affected external method: ' + str(method_that_calls_affected_external_method )
print '\n'

# Based on the lists of methods from points 11. and 12., create a list of unit tests that are optimal test suite for changes in newer commit against older commit (the list of tests is returned in format readable by pom.xml)
resulted_optimal_unit_tests_suite = browse_for_an_optimal_unit_tests_suite(resulted_list_of_affected_methods, resulted_list_of_normalized_files_of_changed_methods, resulted_list_of_methods_that_calls_changed_internal_method, resulted_list_of_methods_that_calls_changed_external_method)

print '13) Based on the lists of methods from points 11. and 12., create a list of unit tests that are optimal test suite for changes in newer commit against older commit (the list of tests is returned in format readable by pom.xml) \n'
for unit_test_to_be_executed in resulted_optimal_unit_tests_suite:
	print '\t unit test to be executed: ' + str(unit_test_to_be_executed)		
print '\n'

# Update pom.xml file with names of unit test to be executed
update_pom(resulted_optimal_unit_tests_suite)

print '14) Update pom.xml file with names of unit test to be executed\n'
print '\t updated pom.xml with optimal test suite is saved in: ' + str(dir_repository_updated_pom_file)