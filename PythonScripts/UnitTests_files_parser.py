#!/usr/bin/env python

from __future__ import division
from ConstantsDefinition import *
from AuxiliaryScripts import *

# /************************************************************
#					parse Unit test files 	
# ************************************************************/
def UnitTests_files_parser(List_of_unitTests):
	returned_List = []
	for unitTest_file in List_of_unitTests: 
		#print 'Source Code Files:'
		
		lines_unitTest = [line.rstrip('\n') for line in open(unitTest_file)]
		
		number_of_tests = 0
		
		for element in lines_unitTest:
			if elem_start in element:
				number_of_tests = number_of_tests + 1
		if (number_of_tests == 0):
			continue
			
		print 'Test file name: ' + unitTest_file
		print 'number_of_tests: ' + str(number_of_tests)
		
		for i in range(0, number_of_tests):
			
			for element in lines_unitTest:
				if elem_start not in element:
					header_id_start = -1
					continue
				else:
					# if first line in a header contains either "* Auxiliary piece of code" or "Class Attributes" string - skip this header
					# if the above - do_execute = False
					if (elem_skip_header_1 in lines_unitTest[lines_unitTest.index(element)+1] or elem_skip_header_2 in lines_unitTest[lines_unitTest.index(element)+1]):
						header_id_skip = lines_unitTest.index(element)+1
						number_of_tests = number_of_tests - 1
						del lines_unitTest[0: header_id_skip]
						#print 'number_of_tests:' + str(number_of_tests)
						do_execute = False
						break
					# if first line in a header contains "Method Name" - copy this line ID and process the header further
					# if the above - do_execute = True
					if elem_ok_header_test in lines_unitTest[lines_unitTest.index(element)+1]:
						unitTest_file_temp = unitTest_file
						#extract filenames from path
						unitTest_file_temp = filename_fromPath(unitTest_file)
						#remove java extension from a file's name 
						unitTest_file_temp = unitTest_file_temp.replace("java", "")
						# add the unit test file name to the test name
						temp_lines_unitTest_header_elem_List = lines_unitTest[lines_unitTest.index(element)+1].split(':')
						unitTest_file_with_testName = unitTest_file_temp + (temp_lines_unitTest_header_elem_List[1])
						unitTest_file_with_testName = unitTest_file_with_testName.replace(" ", "")
						lines_unitTest[lines_unitTest.index(element)+1] = (temp_lines_unitTest_header_elem_List[0]) +": " + unitTest_file_with_testName
						header_id_start = lines_unitTest.index(element)
						do_execute = True
						break
			
			if(do_execute):
				for element in lines_unitTest:
					if elem_stop not in element:
						header_id_stop = -1
						continue
					else:
						header_id_stop = lines_unitTest.index(element)
						break

			
				
				if (( header_id_start != -1 ) and ( header_id_stop != -1 )):
				
					# do not process elem_start and elem_stop, hence lines_unitTest[header_id_start+1: header_id_stop] instead of [header_id_start: header_id_stop+1]
					lines_header = lines_unitTest[header_id_start+1: header_id_stop]
					lines_header_concatenated_string = concatenate_list_data(lines_header)
					lines_header_concatenated_list =  lines_header_concatenated_string.split('*')
					
					#print 'lines_header length: ' + (str(len(lines_header)))
					#print 'lines_header_concatenated_list length: ' + (str(len(lines_header_concatenated_list)))
					
					for line in lines_header_concatenated_list:
						
						line = line.replace("\t", "")
						
						returned_List.append(line)
					
					# delete lines from the lines_unitTest list that have been already processed
					del lines_unitTest[0: header_id_stop+1]		
		
	return returned_List