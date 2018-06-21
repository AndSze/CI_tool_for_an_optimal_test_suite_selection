#!/usr/bin/env python

from __future__ import division
import git
import numpy as np
import time
from ConstantsDefinition import *
from AuxiliaryScripts import *
from RepositoryParser_AuxiliaryScripts import *


commit_newer, commit_older = get_commit_from_repository(dir_repository_master, 3, 4)	

print 'commit_newer: ' + str(commit_newer)
print 'commit_older: ' + str(commit_older)

diff_data_head_to_commit = build_git_diff_log_file(commit_newer, 3, 4)

print 'len(diff_data_head_to_commit): ' + str(len(diff_data_head_to_commit))

list_of_modified_files_to_be_processed, list_of_added_files_to_be_processed, list_of_all_changed_files = build_lists_of_files_to_be_processed(commit_newer, 3, 4)

print 'len(list_of_modified_files_to_be_processed): ' + str(len(list_of_modified_files_to_be_processed))
print 'len(list_of_added_files_to_be_processed): ' + str(len(list_of_added_files_to_be_processed))

resulted_array_of_changed_file_blocks = build_resulted_array_of_changed_file_blocks(list_of_modified_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files)

print 'len(resulted_array_of_changed_file_blocks): ' + str(len(resulted_array_of_changed_file_blocks))

resulted_array_of_added_file_blocks = build_resulted_array_of_added_file_blocks(list_of_added_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files)

print 'len(resulted_array_of_added_file_blocks): ' + str(len(resulted_array_of_added_file_blocks))

resulted_array_of_removed_lines = build_resulted_array_of_removed_lines(resulted_array_of_changed_file_blocks, list_of_modified_files_to_be_processed)

print 'len(resulted_array_of_removed_lines): ' + str(len(resulted_array_of_removed_lines))

resulted_array_of_added_lines = build_resulted_array_of_added_lines(resulted_array_of_changed_file_blocks, list_of_modified_files_to_be_processed)

print 'len(resulted_array_of_added_lines): ' + str(len(resulted_array_of_added_lines))

resulted_array_of_added_files = build_resulted_array_of_added_files(resulted_array_of_added_file_blocks, list_of_added_files_to_be_processed)

print 'len(resulted_array_of_added_files): ' + str(len(resulted_array_of_added_files))

resulted_array_of_added_lines_numbers = build_resulted_array_of_added_lines_numbers(resulted_array_of_added_lines, list_of_modified_files_to_be_processed, commit_newer, dir_repository_newer_commit)

for i in resulted_array_of_added_lines_numbers:
	print i

resulted_array_of_removed_lines_numbers = build_resulted_array_of_removed_lines_numbers(resulted_array_of_removed_lines, list_of_modified_files_to_be_processed, commit_older, dir_repository_older_commit)

for i in resulted_array_of_removed_lines_numbers:
	print i
