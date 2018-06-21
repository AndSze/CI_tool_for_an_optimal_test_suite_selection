#!/usr/bin/env python

from __future__ import division
import git
import numpy as np
import time
from ConstantsDefinition import *
from AuxiliaryScripts import *

def build_resulted_array_of_removed_lines_numbers(resulted_array_of_removed_lines, list_of_modified_files_to_be_processed, commit_older, repo_directory_address_diff): 

	start = time.time()
		
	# array of lists of file names along with the method names for functions that have been modified (removed lines) between commits for these files
	resulted_array_of_modified_methods_with_removed_lines = np.empty((len(resulted_array_of_removed_lines),),dtype=object)
	resulted_array_of_modified_methods_with_removed_lines.fill([])
	
	# checkout all files from commit_older to dir_repository_older_commit
	repository = git.Repo(repo_directory_address_diff)
	repository.git.checkout('-f',commit_older)
	
	# build list_of_sourceFiles_from_older_commit that contains only source code files from dir_repository_older_commit_sourceCode
	list_of_sourceFiles_from_older_commit = list_files(dir_repository_older_commit_sourceCode)
	
	returned_list_of_methods = []
	normalized_list_of_sourceFiles_from_older_commit = []
	normalized_list_of_removed_files_to_be_processed = []
	
	# build list_of_sourceFiles_from_older_commit that contains normalized paths for list_of_sourceFiles_from_older_commit
	for source_file in list_of_sourceFiles_from_older_commit: 
		
		index = list_of_sourceFiles_from_older_commit.index(source_file)
		
		source_file = os.path.normpath(source_file)
		source_file = source_file.split(os.sep)
		source_file = source_file[-5: len(source_file)]
		
		normalized_list_of_sourceFiles_from_older_commit.append(source_file)
	
	# build normalized_list_of_modified_files_to_be_processed that contains normalized paths for list_of_modified_files_to_be_processed	
	for file_to_be_processed in list_of_modified_files_to_be_processed: 
	
		index = list_of_modified_files_to_be_processed.index(file_to_be_processed)
		
		file_to_be_processed = os.path.normpath(file_to_be_processed)
		file_to_be_processed = file_to_be_processed.split(os.sep)
		
		file_to_be_processed = file_to_be_processed[-5: len(file_to_be_processed)]
		
		normalized_list_of_removed_files_to_be_processed.append(file_to_be_processed)

	iterator = 0
	element_to_be_found = "Method Name"
	# build resulted_array_of_modified_methods_with_removed_lines
	for source_file in normalized_list_of_sourceFiles_from_older_commit:
	
		source_file_index = normalized_list_of_sourceFiles_from_older_commit.index(source_file)
		
		if source_file not in normalized_list_of_removed_files_to_be_processed:
			continue
		else:
			changed_lines_numbers_for_a_file_list = []
			changed_method_names_for_a_file_list = []
			changed_method_names_for_a_file_list.append(source_file)
			#print 'source_file: ' + str(source_file)
			lines_sourceFile = [line.rstrip('\n') for line in open(list_of_sourceFiles_from_older_commit[source_file_index])]
			index_of_file = normalized_list_of_removed_files_to_be_processed.index(source_file)
			for line in lines_sourceFile:
				line_ID = lines_sourceFile.index(line)
				line = line.replace('\t', "")
				line = line.replace(' ', "")
				lines_sourceFile[line_ID] = line
				#print 'source code line: ' + str(lines_sourceFile[line_ID])
		
		# build changed_lines_numbers_for_a_file_list		
		for line in resulted_array_of_removed_lines[index_of_file]:
			line = line.replace('\t', "")
			line = line.replace(' ', "")
		
			if line in lines_sourceFile:
				index_of_line_in_source_file = lines_sourceFile.index(line)		
				changed_lines_numbers_for_a_file_list.append(index_of_line_in_source_file)
				#print 'index_of_line_in_source_file: ' + str(index_of_line_in_source_file)
		
		# build changed_method_names_for_a_file_list
		if len(changed_lines_numbers_for_a_file_list) > 0:	
		
			# call build_element_line_numbers_list_for_a_file
			list_of_method_name_line_numbers, list_of_changed_methods = build_element_line_numbers_list_for_a_file(list_of_sourceFiles_from_older_commit[source_file_index], element_to_be_found)
			
			for method_name_line_number in list_of_method_name_line_numbers:
				for changed_line_number in changed_lines_numbers_for_a_file_list:
					changed_method_name = ''
					if list_of_method_name_line_numbers.index(method_name_line_number) == 0 and len(list_of_method_name_line_numbers) > 1 :
						if changed_line_number < method_name_line_number:
							changed_method_name = 'Class Attributes'
					if changed_line_number > method_name_line_number:
						changed_method_name = list_of_changed_methods[list_of_method_name_line_numbers.index(method_name_line_number)]
					if (changed_method_name != '') and (changed_method_name not in changed_method_names_for_a_file_list):
						changed_method_names_for_a_file_list.append(changed_method_name)
					else:
						continue

			resulted_array_of_modified_methods_with_removed_lines[iterator] = changed_method_names_for_a_file_list
			iterator = iterator + 1
			
	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_modified_methods_with_removed_lines) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_modified_methods_with_removed_lines

def build_resulted_array_of_added_lines_numbers(resulted_array_of_added_lines, list_of_modified_files_to_be_processed, commit_newer, repo_directory_address_diff): 

	start = time.time()
		
	# array of lists of file names along with the method names for functions that have been modified (added lines) between commits for these files
	resulted_array_of_modified_methods_with_added_lines = np.empty((len(resulted_array_of_added_lines),),dtype=object)
	resulted_array_of_modified_methods_with_added_lines.fill([])
	
	# checkout all files from commit_newer to dir_repository_newer_commit
	repository = git.Repo(repo_directory_address_diff)
	repository.git.checkout('-f',commit_newer)
	
	# build list_of_sourceFiles_from_newer_commit that contains only source code files from dir_repository_newer_commit_sourceCode
	list_of_sourceFiles_from_newer_commit = list_files(dir_repository_newer_commit_sourceCode)
	
	returned_list_of_methods = []
	normalized_list_of_sourceFiles_from_newer_commit = []
	normalized_list_of_modified_files_to_be_processed = []
	
	# build normalized_list_of_sourceFiles_from_newer_commit that contains normalized paths for list_of_sourceFiles_from_newer_commit
	for source_file in list_of_sourceFiles_from_newer_commit: 
		
		index = list_of_sourceFiles_from_newer_commit.index(source_file)
		
		source_file = os.path.normpath(source_file)
		source_file = source_file.split(os.sep)
		source_file = source_file[-5: len(source_file)]
		
		normalized_list_of_sourceFiles_from_newer_commit.append(source_file)
	
	# build normalized_list_of_modified_files_to_be_processed that contains normalized paths for list_of_modified_files_to_be_processed
	for file_to_be_processed in list_of_modified_files_to_be_processed: 
	
		index = list_of_modified_files_to_be_processed.index(file_to_be_processed)
		
		file_to_be_processed = os.path.normpath(file_to_be_processed)
		file_to_be_processed = file_to_be_processed.split(os.sep)
		file_to_be_processed = file_to_be_processed[-5: len(file_to_be_processed)]
		
		normalized_list_of_modified_files_to_be_processed.append(file_to_be_processed)
	
	iterator = 0
	element_to_be_found = "Method Name"
	# build resulted_array_of_modified_methods_with_added_lines
	for source_file in normalized_list_of_sourceFiles_from_newer_commit:
	
		source_file_index = normalized_list_of_sourceFiles_from_newer_commit.index(source_file)
		
		if source_file not in normalized_list_of_modified_files_to_be_processed:
			continue
		else:
			changed_lines_numbers_for_a_file_list = []
			changed_method_names_for_a_file_list = []
			changed_method_names_for_a_file_list.append(source_file)
			#print 'source_file: ' + str(source_file)
			lines_sourceFile = [line.rstrip('\n') for line in open(list_of_sourceFiles_from_newer_commit[source_file_index])]
			index_of_file = normalized_list_of_modified_files_to_be_processed.index(source_file)
			for line in lines_sourceFile:
				line_ID = lines_sourceFile.index(line)
				line = line.replace('\t', "")
				line = line.replace(' ', "")
				lines_sourceFile[line_ID] = line
				#print 'source code line: ' + str(lines_sourceFile[line_ID])
		
		# build changed_lines_numbers_for_a_file_list		
		for line in resulted_array_of_added_lines[index_of_file]:
			line = line.replace('\t', "")
			line = line.replace(' ', "")
		
			if line in lines_sourceFile:
				index_of_line_in_source_file = lines_sourceFile.index(line)		
				changed_lines_numbers_for_a_file_list.append(index_of_line_in_source_file)
				#print 'index_of_line_in_source_file: ' + str(index_of_line_in_source_file)
		
		# build changed_method_names_for_a_file_list
		if len(changed_lines_numbers_for_a_file_list) > 0:	
		
			# call build_element_line_numbers_list_for_a_file
			list_of_method_name_line_numbers, list_of_changed_methods = build_element_line_numbers_list_for_a_file(list_of_sourceFiles_from_newer_commit[source_file_index], element_to_be_found)
			
			for method_name_line_number in list_of_method_name_line_numbers:
				for changed_line_number in changed_lines_numbers_for_a_file_list:
					changed_method_name = ''
					if list_of_method_name_line_numbers.index(method_name_line_number) == 0 and len(list_of_method_name_line_numbers) > 1 :
						if changed_line_number < method_name_line_number:
							changed_method_name = 'Class Attributes'
					if changed_line_number > method_name_line_number:
						changed_method_name = list_of_changed_methods[list_of_method_name_line_numbers.index(method_name_line_number)]
					if (changed_method_name != '') and (changed_method_name not in changed_method_names_for_a_file_list):
						changed_method_names_for_a_file_list.append(changed_method_name)
					else:
						continue

			resulted_array_of_modified_methods_with_added_lines[iterator] = changed_method_names_for_a_file_list
			iterator = iterator + 1
	
	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_added_lines_numbers) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_modified_methods_with_added_lines

# /************************************************************
# create a list of line numbers for the lines that contain an element given as the input parameter
# if two returned parameters are expected, the second list contains previously filtered textual content of the lines from first list
# ************************************************************/	
def build_element_line_numbers_list_for_a_file(file_dir, element_to_be_found):

	# read lines from a file from file_dir
	lines_file = [line.rstrip('\n') for line in open(file_dir)]

	resulted_list_of_line_numbers = []
	resulted_list_of_text_in_line_numbers = []
	
	# build resulted_list_of_line_numbers
	# build resulted_list_of_text_in_line_numbers
	for line in lines_file:
		if (element_to_be_found not in line):
			continue
		else:
			line_index = lines_file.index(line)
			if 'Auxiliary piece of code' in lines_file[line_index - 1]:
				break;
			else:
				resulted_list_of_line_numbers.append(line_index)
				# remove unnecessary characters from the line that contains changed_method_name
				changed_method_name = lines_file[lines_file.index(line)]
				changed_method_name = changed_method_name.replace('\t', "")
				changed_method_name = changed_method_name.replace(element_to_be_found, "")
				changed_method_name = changed_method_name.replace('*', "")
				colon_id = changed_method_name.index(':')
				changed_method_name = changed_method_name[(colon_id + 2):]	
			
			resulted_list_of_text_in_line_numbers.append(changed_method_name)

	return [resulted_list_of_line_numbers, resulted_list_of_text_in_line_numbers]

# /************************************************************
# based on lists of added blocks in files, create an array that contains code lines for the files that have been added in newer commit
# this function filters the added lines and does not include commentaries, headers or empty lines in the resulted array 
# ************************************************************/	
def build_resulted_array_of_added_files(resulted_array_of_added_file_blocks, list_of_added_files_to_be_processed):
	
	start = time.time()
	
	# array of lists that will store all lines from the new file that have been added in the commits under analysis, changes for each file are stored at different array indexes
	# indexes of the lists in the resulted_array_of_added_files array in are the same as indexes of the files that contain these lists in the list_of_added_files_to_be_processed list
	resulted_array_of_added_files = np.empty((len(list_of_added_files_to_be_processed),),dtype=object)
	resulted_array_of_added_files.fill([])
	
	# build resulted_array_of_added_files 
	iterator = 0
	for file_block in resulted_array_of_added_file_blocks:			
		added_lines_list = []	
		for line in file_block:
			if len(line) > 0:
				# build list of lines that have been added - what is indicated by '+' in the diff log file
				if line[0] == '+':
					temp_index = file_block.index(line)
					
					temp_line = line.replace('\t', "")
					temp_line = temp_line.replace('+', "")
					temp_line = temp_line.replace(" ", "")
					
					# process line if it contains at least 2 characters
					if len(temp_line) > 1:
						#if a line contains either commentary or header that has been changed, do not add it to added_lines_list
						if temp_line[0] == '*' or temp_line[0] == '/':
							continue
						#if a line contains package name or imported dependencies, do not add it to added_lines_list
						elif 'package' in temp_line:
							continue
						elif 'import' in temp_line:
							continue
					# if a line contains less than 2 characters, do not add it to added_lines_list
					else:
						continue
						
					# do not add a changed line if it already exists in added_lines_list
					if file_block[temp_index] in added_lines_list:
						continue
					# add line to added_lines_list
					else:
						file_block[temp_index] = file_block[temp_index].replace('+', " ")
						added_lines_list.append(file_block[temp_index]) 
						#print 'added_lines_list.append(file_block[temp_index]): ' + str(file_block[temp_index])
		resulted_array_of_added_files[iterator] = added_lines_list
		iterator = iterator + 1

	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_added_files) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_added_files		
		
# /************************************************************
# based on lists of changed blocks in files, create an array that contains code lines that have been added
# this function filters the added lines and does not include commentaries, headers or empty lines in the resulted array 
# ************************************************************/	
def build_resulted_array_of_added_lines(resulted_array_of_changed_file_blocks, list_of_modified_files_to_be_processed):

	start = time.time()

	# array of lists that will store lines that have been changed between the file versions under analysis, changes for each file are stored at different array indexes
	# indexes of the lists in the resulted_array_of_added_lines array in are the same as indexes of the files that contain these lists in the list_of_modified_files_to_be_processed list
	resulted_array_of_added_lines = np.empty((len(list_of_modified_files_to_be_processed),),dtype=object)
	resulted_array_of_added_lines.fill([])
	
	# build resulted_array_of_added_lines
	iterator = 0
	for file_block in resulted_array_of_changed_file_blocks:			
		changed_lines_list = []	
		for line in file_block:
			if len(line) > 0:
				# build list of lines that have changed - what is indicated by '-' in the diff log
				if line[0] == '-' and line[0:3] != '---':

					temp_index = file_block.index(line)
					
					temp_line = line.replace('\t', "")
					temp_line = temp_line.replace('-', "")
					temp_line = temp_line.replace(" ", "")
					
					# if a line contains either commentary or header that has been removed, do not add it to changed_lines_list
					if len(temp_line) > 1:
						if temp_line[0] == '*' or temp_line[0] == '/':
							continue
					# if a line contains less than 2 characters, do not add it to changed_lines_list
					else:
						continue
						
					# do not add a removed line if it already exists in changed_lines_list
					if file_block[temp_index] in changed_lines_list:
						continue
					# add line to changed_lines_list
					else:
						file_block[temp_index] = file_block[temp_index].replace('-', " ")
						changed_lines_list.append(file_block[temp_index])
						#print 'changed_lines_list.append(file_block[temp_index]): ' + str(file_block[temp_index])
		
		resulted_array_of_added_lines[iterator] = changed_lines_list
		iterator = iterator + 1
		
	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_added_lines) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_added_lines
	
# /************************************************************
# based on lists of changed blocks in files, create an array that contains code lines that have been removed
# this function filters the removed lines and does not include commentaries, headers or empty lines in the resulted array 
# ************************************************************/	
def build_resulted_array_of_removed_lines(resulted_array_of_changed_file_blocks, list_of_modified_files_to_be_processed):

	start = time.time()
	
	# array of lists that will store lines that have been removed from the file under analysis, changes for each file are stored at different array indexes
	# indexes of the lists in the resulted_array_of_removed_lines array in are the same as indexes of the files that contain these lists in the list_of_modified_files_to_be_processed list
	resulted_array_of_removed_lines = np.empty((len(list_of_modified_files_to_be_processed),),dtype=object)
	resulted_array_of_removed_lines.fill([])

	# buildresulted_array_of_removed_lines
	iterator = 0
	for file_block in resulted_array_of_changed_file_blocks:			
		removed_lines_list = []	
		changed_lines_list = []	
		for line in file_block:
			if len(line) > 0:
				# build list of lines that have been removed - what is indicated by '+' in the diff log
				if line[0] == '+':
					temp_index = file_block.index(line)
					
					temp_line = line.replace('\t', "")
					temp_line = temp_line.replace('+', "")
					temp_line = temp_line.replace(" ", "")
					
					#if a line contains either commentary or header that has been changed, do not add it to removed_lines_list
					if len(temp_line) > 1:
						if temp_line[0] == '*' or temp_line[0] == '/':
							continue
					# if a line contains less than 2 characters, do not add it to removed_lines_list
					else:
						continue
						
					# do not add a changed line if it already exists in removed_lines_list
					if file_block[temp_index] in removed_lines_list:
						continue
					# add line to removed_lines_list
					else:
						file_block[temp_index] = file_block[temp_index].replace('+', " ")
						removed_lines_list.append(file_block[temp_index]) 
						#print 'removed_lines_list.append(file_block[temp_index]): ' + str(file_block[temp_index])
		resulted_array_of_removed_lines[iterator] = removed_lines_list
		iterator = iterator + 1
	
	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_removed_lines) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_removed_lines

# /************************************************************
# based on the git diff log file create the array that contain lists of added blocks within all added files between commits 
# ************************************************************/	
def build_resulted_array_of_added_file_blocks(list_of_added_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files):
	
	start = time.time()

	# array of fileblocks lists that are listed in the git diff log, lists of fileblocks for each added file are stored at different array indexes
	# indexes of the lists in the resulted_array_of_added_file_blocks array in are the same as indexes of the files that contain these lists in the list_of_added_files_to_be_processed list
	resulted_array_of_added_file_blocks = np.empty((len(list_of_added_files_to_be_processed),),dtype=object)
	resulted_array_of_added_file_blocks.fill([])
	
	# build resulted_array_of_added_file_blocks
	iterator = 0
	for file in list_of_added_files_to_be_processed:
		if '/src/main/java' in file or '/src/test/java' in file:
			if file in list_of_all_changed_files:
				index = list_of_all_changed_files.index(file)
				for line in diff_data_head_to_commit:
					# search for the modified/added file name in diff_data_head_to_commit
					if file in line:
						block_start_id = diff_data_head_to_commit.index(line)
					# check unless modified file is the last file in list_of_all_changed_files
					if( len(list_of_all_changed_files) != (index + 1)):
						# search for next changed file name in diff_data_head_to_commit
						if list_of_all_changed_files[index + 1] in line:
							block_stop_id = diff_data_head_to_commit.index(line) - 1
					else:
						block_stop_id = len(diff_data_head_to_commit)
				# all changes in one file - iterator is for "fileblocks" from the git diff log
				resulted_array_of_added_file_blocks[iterator] = diff_data_head_to_commit[block_start_id: block_stop_id]
				iterator = iterator + 1
			
	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_added_file_blocks from the git diff log file) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_added_file_blocks

# /************************************************************
# based on the git diff log file create the array that contain lists of changed blocks within all modified files between commits 
# ************************************************************/
def build_resulted_array_of_changed_file_blocks(list_of_modified_files_to_be_processed, diff_data_head_to_commit, list_of_all_changed_files):

	start = time.time()
	
	# array of fileblocks lists that are listed in the git diff log, lists of fileblocks for each modified file are stored at different array indexes
	# indexes of the lists in the resulted_array_of_changed_file_blocks array in are the same as indexes of the files that contain these lists in the list_of_modified_files_to_be_processed list
	resulted_array_of_changed_file_blocks=np.empty((len(list_of_modified_files_to_be_processed),),dtype=object)
	resulted_array_of_changed_file_blocks.fill([])
		
	# build resulted_array_of_changed_file_blocks
	iterator = 0
	for file in list_of_modified_files_to_be_processed:
		if '/src/main/java' in file or '/src/test/java' in file:
			if file in list_of_all_changed_files:
				index = list_of_all_changed_files.index(file)
				for line in diff_data_head_to_commit:
					# search for the modified/added file name in diff_data_head_to_commit
					if file in line:
						block_start_id = diff_data_head_to_commit.index(line)
					# check unless modified file is the last file in list_of_all_changed_files
					if( len(list_of_all_changed_files) != (index + 1)):
						# search for next changed file name in diff_data_head_to_commit
						if list_of_all_changed_files[index + 1] in line:
							block_stop_id = diff_data_head_to_commit.index(line) - 1
					else:
						block_stop_id = len(diff_data_head_to_commit)
				# all changes in one file - iterator is for "fileblocks" from the git diff log
				resulted_array_of_changed_file_blocks[iterator] = diff_data_head_to_commit[block_start_id: block_stop_id]
				iterator = iterator + 1

	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_changed_file_blocks from the git diff log file) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return resulted_array_of_changed_file_blocks

# /************************************************************
# create the lists that contain files that require further processing based on updates made to them between commits 	
# ************************************************************/
def build_lists_of_files_to_be_processed(commit_newer, head_init, head_init_minus_x):
	
	start = time.time()
	
	if ( head_init != 0):
		head_init_minus_x = head_init_minus_x - head_init
			
	list_of_all_changed_files = []
	ListAdded_detail = []
	ListDeleted_detail = []
	ListModified_detail = []
	
	# to get all files that have changed between commits to be analyzed - ignore white space at the end of line, empty lines, renamed files and also copied files
	for item in commit_newer.diff(str(commit_newer)+'~'+str(head_init_minus_x), create_patch=True, ignore_blank_lines=True, 
							ignore_space_at_eol=True, diff_filter='cr'):
		list_of_all_changed_files.append(str(item.a_path))
		
	# add all files that have been added to ListAdded_detail - ignore white space at the end of line, empty lines, renamed files and also copied files
	for diff_added in commit_newer.diff(str(commit_newer)+'~'+str(head_init_minus_x), create_patch=True, ignore_blank_lines=True, 
							ignore_space_at_eol=True, diff_filter='cr').iter_change_type('A'):					
		diff_added = str(diff_added).split('\n')
		# append only first line that contains file name - diff_added[0]
		ListAdded_detail.append(str(diff_added[0]))

	# add all files that have been deleted to ListDeleted_detail - ignore white space at the end of line, empty lines, renamed files and also copied files
	for diff_deleted in commit_newer.diff(str(commit_newer)+'~'+str(head_init_minus_x), create_patch=True, ignore_blank_lines=True, 
							ignore_space_at_eol=True, diff_filter='cr').iter_change_type('D'):
		diff_deleted = str(diff_deleted).split('\n')
		# append only first line that contains file name - diff_deleted[0]
		ListDeleted_detail.append(str(diff_deleted[0]))

	# add all files that have been modified to ListModified_detail - ignore white space at the end of line, empty lines, renamed files and also copied files
	for diff_modified in commit_newer.diff(str(commit_newer)+'~'+str(head_init_minus_x), create_patch=True, ignore_blank_lines=True, 
							ignore_space_at_eol=True, diff_filter='cr').iter_change_type('M'):
		diff_modified = str(diff_modified).split('\n')
		# append only first line that contains file name - diff_modified[0]
		ListModified_detail.append(str(diff_modified[0]))		
	
	print 'len(list_of_all_changed_files): ' + str(len(list_of_all_changed_files))
	print 'len(ListAdded_detail): ' + str(len(ListAdded_detail))
	print 'len(ListDeleted_detail): ' + str(len(ListDeleted_detail))
	print 'len(ListModified_detail): ' + str(len(ListModified_detail))
	list_of_modified_files_to_be_processed = []
	list_of_added_files_to_be_processed = []
	
	for file in ListAdded_detail:
		if '/src/main/java' in file or '/src/test/java' in file:
			list_of_added_files_to_be_processed.append(file)
			
	iterator = 0
	# build list_of_modified_files_to_be_processed
	for changed_file in list_of_all_changed_files:
		if '/src/main/java' in changed_file or '/src/test/java' in changed_file:
			# file has been deleted, hence do not process it further
			if changed_file in ListDeleted_detail:
				continue
			elif changed_file in ListModified_detail:
				list_of_modified_files_to_be_processed.append(changed_file)		
		# files added to the repository have 'None' in list_of_all_changed_files, because base commit is the older one (hence paths for the deleted files are accessible, whereas paths for the added files are not) 
		# for the purpose of further processing the git diff log file, paths for the added files are changed from 'None' to the valid paths in list_of_all_changed_files
		if 'None' in changed_file:
			list_of_all_changed_files[list_of_all_changed_files.index(changed_file)] = ListAdded_detail[iterator]
			iterator = iterator + 1
			
	for file in list_of_modified_files_to_be_processed:
		print 'Modified file to be processed: ' + str(file)		
	
	for file in list_of_added_files_to_be_processed:
		print 'Added file to be processed: ' + str(file)	
		
	end = time.time()
	print '\n\n\nbuilding arrays (resulted_array_of_files_to_be_processed) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return [list_of_modified_files_to_be_processed, list_of_added_files_to_be_processed, list_of_all_changed_files]
	
# /************************************************************
# create the diff log file that outlines changes between commits	
# ************************************************************/		
def build_git_diff_log_file(commit_newer, head_init, head_init_minus_x):

	start = time.time()
	
	if ( head_init != 0):
		head_init_minus_x = head_init_minus_x - head_init
		
	# Git ignore white space at the end of line, empty lines, renamed files and also copied files
	diff_index = commit_newer.diff(str(commit_newer)+'~'+str(head_init_minus_x), create_patch=True, ignore_blank_lines=True, 
								ignore_space_at_eol=True, diff_filter='cr')
							 
	diff_output_text = reduce(lambda x, y: str(x)+str(y), diff_index)
	
	myFile = open('git_diff_log.txt', 'w')
	myFile.write(str(diff_output_text))
	myFile.close()
	
	diff_data_head_to_commit = diff_output_text.split('\n')
	
	end = time.time()
	print '\n\n\nbuilding the git diff log file (diff_data_head_to_commit) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	return diff_data_head_to_commit

# /************************************************************
#  gets the commits IDs to be analyzed from repository
# ************************************************************/
def get_commit_from_repository(repo_directory_address, head_init, head_init_minus_x):
	
	start = time.time()
		
	# create local repository variable in the code based on repo_directory_address
	repository = git.Repo(repo_directory_address)
	
	# get latest (head) commitID
	head_commitID = repository.head.commit
	
	# get commit ID of the commit that is the head or close to the head (commit_newer)
	if ( head_init == 0):
		commit_newer = repository.commit(head_commitID)
	else:
		commit_newer = repository.commit(str(head_commitID)+'~'+str(head_init))
		head_init_minus_x = head_init_minus_x - head_init

	# get commit ID of the base commit that is older than commit_newer (commit_older)
	commit_older = repository.commit(str(commit_newer)+'~'+str(head_init_minus_x))
	print 'comparing the newer commit: ' + str(commit_newer) + ' against the older: ' + str(commit_older)
	
	end = time.time()
	print '\n\n\ngetting the commits IDs (commit_older & commit_newer) execution time\t\t\t: ' + str((end - start)) + '\n\n\n'
	
	
	return [commit_newer, commit_older]
	
	
