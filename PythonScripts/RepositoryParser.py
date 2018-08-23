#!/usr/bin/env python

from __future__ import division
import git
import numpy as np
import time
from ConstantsDefinition import *
from AuxiliaryScripts import *
from RepositoryParser_AuxiliaryScripts import *

commit_newer_id = 0
commit_older_id = 2

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
print '\tlen(list_of_modified_files_to_be_processed): ' + str(len(list_of_modified_files_to_be_processed))
print '\tlen(list_of_added_files_to_be_processed): ' + str(len(list_of_added_files_to_be_processed))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods with added lines
resulted_array_of_added_lines_in_changed_file_blocks = build_resulted_array_of_changed_file_blocks(list_of_modified_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files)

print '4) Parse git diff log to separate file blocks (method body) for methods with added lines \n'
print '\tlen(resulted_array_of_added_lines_in_changed_file_blocks): ' + str(len(resulted_array_of_added_lines_in_changed_file_blocks))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods with removed lines
resulted_array_of_removed_lines_in_changed_file_blocks = build_resulted_array_of_removed_lines(resulted_array_of_added_lines_in_changed_file_blocks, list_of_modified_files_to_be_processed)

print '5) Parse git diff log to separate file blocks (method body) for methods with removed lines \n'
print '\t len(resulted_array_of_removed_lines_in_changed_file_blocks): ' + str(len(resulted_array_of_removed_lines_in_changed_file_blocks))
print '\n'

# Parse git diff log to separate file blocks (method body) for methods that have been added
resulted_array_of_added_lines_in_added_file_blocks = build_resulted_array_of_added_file_blocks(list_of_added_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files)

print '6) Parse git diff log to separate file blocks (method body) for methods that have been added \n'
print '\t len(resulted_array_of_added_lines_in_added_file_blocks): ' + str(len(resulted_array_of_added_lines_in_added_file_blocks))
print '\n'

'''
# Parse file blocks (method body) to separate added lines
resulted_array_of_added_lines = build_resulted_array_of_added_lines(resulted_array_of_added_lines_in_changed_file_blocks, list_of_modified_files_to_be_processed)
for i in resulted_array_of_added_lines:
	print 'array_of_added_line: ' + str(i)
	
print 'len(resulted_array_of_added_lines): ' + str(len(resulted_array_of_added_lines))

resulted_array_of_added_files = build_resulted_array_of_added_files(resulted_array_of_added_lines_in_added_file_blocks, list_of_added_files_to_be_processed)

print 'len(resulted_array_of_added_files): ' + str(len(resulted_array_of_added_files))
'''

resulted_array_of_methods_with_added_lines_numbers = build_resulted_array_of_added_lines_numbers(resulted_array_of_added_lines_in_changed_file_blocks, list_of_modified_files_to_be_processed, commit_newer, dir_repository_newer_commit)

for methods_with_added_lines_numbers in resulted_array_of_methods_with_added_lines_numbers:
	print '\t methods_with_added_lines_numbers' + str(methods_with_added_lines_numbers)
	
resulted_array_of_methods_with_removed_lines_numbers = build_resulted_array_of_removed_lines_numbers(resulted_array_of_removed_lines_in_changed_file_blocks, list_of_modified_files_to_be_processed, commit_older, dir_repository_older_commit)

for methods_with_removed_lines_numbers in resulted_array_of_methods_with_removed_lines_numbers:
	print '\t methods_with_added_lines_numbers' + str(methods_with_removed_lines_numbers)
	
resulted_array_of_methods_in_added_files = build_resulted_array_of_added_lines_numbers(resulted_array_of_added_lines_in_added_file_blocks, list_of_added_files_to_be_processed, commit_newer, dir_repository_newer_commit)

for methods_in_added_files in resulted_array_of_methods_in_added_files:
	print '\t methods_with_removed_lines_numbers' + str(methods_in_added_files)
	
resulted_array_of_affected_methods = []

for affected_method in resulted_array_of_methods_with_added_lines_numbers:
	resulted_array_of_affected_methods.append(affected_method)

for affected_method in resulted_array_of_methods_with_removed_lines_numbers:
	if(affected_method not in resulted_array_of_affected_methods):
		resulted_array_of_affected_methods.append(affected_method)

for affected_method in resulted_array_of_methods_in_added_files:
	if(affected_method not in resulted_array_of_affected_methods):
		resulted_array_of_affected_methods.append(affected_method)
		
for i in resulted_array_of_affected_methods:
	print 'affected normalized file and affected method inside the file: ' + str(i)
	
resulted_list_of_changed_methods, resulted_list_of_normalized_files_of_changed_methods = build_resulted_list_of_changed_methods(resulted_array_of_affected_methods)

for i in resulted_list_of_changed_methods:
	print 'changed method: ' + str(i)
	
resulted_list_of_methods_that_calls_changed_internal_method, resulted_list_of_methods_that_calls_changed_external_method = build_resulted_list_of_methods_that_calls_changed_method(resulted_array_of_affected_methods)

for i in resulted_list_of_methods_that_calls_changed_internal_method:
	print 'method that calls changed internal method: ' + str(i)
	
for i in resulted_list_of_methods_that_calls_changed_external_method:
	print 'method that calls changed_external method: ' + str(i)
	
resulted_optimal_unit_tests_suite = browse_for_an_optimal_unit_tests_suite(resulted_list_of_changed_methods, resulted_list_of_normalized_files_of_changed_methods, resulted_list_of_methods_that_calls_changed_internal_method, resulted_list_of_methods_that_calls_changed_external_method)

for i in resulted_optimal_unit_tests_suite:
	print 'test to be executed: ' + str(i)				