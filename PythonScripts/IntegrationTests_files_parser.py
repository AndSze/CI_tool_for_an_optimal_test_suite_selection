#!/usr/bin/env python

from __future__ import division
from ConstantsDefinition import *
from AuxiliaryScripts import *

# /************************************************************
#					parse Integration test files 	
# ************************************************************/
def IntegrationTests_files_parser(List_of_integrationTests):
	returned_List = []
	for integrationTest_file in List_of_integrationTests: 
		#print 'Source Code Files:'
		
		lines_integrationTest = [line.rstrip('\n') for line in open(integrationTest_file)]
		
		number_of_tests = 0
		
		for element in lines_integrationTest:
			if elem_start in element:
				number_of_tests = number_of_tests + 1
		if (number_of_tests == 0):
			continue
			
		print 'Test file name: ' + integrationTest_file
		print 'number_of_tests: ' + str(number_of_tests)
		
		for i in range(0, number_of_tests):
			
			for element in lines_integrationTest:
				if elem_start not in element:
					header_id_start = -1
					continue
				else:
					# if first line in a header contains either "* Auxiliary piece of code" or "Class Attributes" string - skip this header
					# if the above - do_execute = False
					if (elem_skip_header_1 in lines_integrationTest[lines_integrationTest.index(element)+1] or elem_skip_header_2 in lines_integrationTest[lines_integrationTest.index(element)+1]):
						header_id_skip = lines_integrationTest.index(element)+1
						number_of_tests = number_of_tests - 1
						del lines_integrationTest[0: header_id_skip]
						#print 'number_of_tests:' + str(number_of_tests)
						do_execute = False
						break
					# if first line in a header contains "Method Name" - copy this line ID and process the header further
					# if the above - do_execute = True
					if elem_ok_header_test in lines_integrationTest[lines_integrationTest.index(element)+1]:
						integrationTest_file_temp = integrationTest_file
						#extract filenames from path
						integrationTest_file_temp = filename_fromPath(integrationTest_file)
						#remove java extension from a file's name 
						integrationTest_file_temp = integrationTest_file_temp.replace("java", "")
						# add the integration test file name to the test name
						temp_lines_integrationTest_header_elem_List = lines_integrationTest[lines_integrationTest.index(element)+1].split(':')
						integrationTest_file_with_testName = integrationTest_file_temp + (temp_lines_integrationTest_header_elem_List[1])
						integrationTest_file_with_testName = integrationTest_file_with_testName.replace(" ", "")
						lines_integrationTest[lines_integrationTest.index(element)+1] = (temp_lines_integrationTest_header_elem_List[0]) +": " + integrationTest_file_with_testName
						header_id_start = lines_integrationTest.index(element)
						do_execute = True
						break
			
			if(do_execute):
				for element in lines_integrationTest:
					if elem_stop not in element:
						header_id_stop = -1
						continue
					else:
						header_id_stop = lines_integrationTest.index(element)
						break

			
				
				if (( header_id_start != -1 ) and ( header_id_stop != -1 )):
				
					# do not process elem_start and elem_stop, hence lines_integrationTest[header_id_start+1: header_id_stop] instead of [header_id_start: header_id_stop+1]
					lines_header = lines_integrationTest[header_id_start+1: header_id_stop]
					lines_header_concatenated_string = concatenate_list_data(lines_header)
					lines_header_concatenated_list =  lines_header_concatenated_string.split('*')
					
					#print 'lines_header length: ' + (str(len(lines_header)))
					#print 'lines_header_concatenated_list length: ' + (str(len(lines_header_concatenated_list)))
					
					for line in lines_header_concatenated_list:
						
						line = line.replace("\t", "")
						
						returned_List.append(line)
					
					# delete lines from the lines_integrationTest list that have been already processed
					del lines_integrationTest[0: header_id_stop+1]		
		
	return returned_List