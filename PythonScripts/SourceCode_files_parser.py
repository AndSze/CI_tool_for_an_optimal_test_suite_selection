#!/usr/bin/env python

from __future__ import division
import numpy as np
from ConstantsDefinition import *
from AuxiliaryScripts import *

# /************************************************************
#					parse Source Code files 	
# ************************************************************/
def SourceCode_files_parser(List_of_sourceFiles):

	# array of lists of file names along with the headers content for each method within these files, lists for each method are stored at different array indexes
	# array[][0] - (list) normalized file name
	# array[][1] - (int) number of a header line that contains the 'method name' field 
	# array[][2] - (string) method name
	# array[][3:] - (string) content of all fields in the header for a method name - number of elements in the array can vary, because different headers contain variable number of fields
	resulted_array_of_method_headers=np.empty((len(List_of_sourceFiles),),dtype=object)
	resulted_array_of_method_headers.fill([])
	
	iterator = 0
	for source_file in List_of_sourceFiles: 
		#print 'Source Code Files:'
		
		# if there will be the following assignment made: lines_source = lines_source_fixed, these two variables will be interpreted as one variable that value equals to value of the last modified one 
		lines_source_fixed = [line.rstrip('\n') for line in open(source_file)]
		lines_source = [line.rstrip('\n') for line in open(source_file)]

		number_of_headers = 0
		
		# count headers in lines_source
		for element in lines_source:
			if elem_start in element:
				number_of_headers = number_of_headers + 1
		if (number_of_headers == 0):
			continue
		
		# normalize the source_file path
		source_file = os.path.normpath(source_file)
		source_file = source_file.split(os.sep)
		source_file = source_file[-5: len(source_file)]
		
		# iterate through number_of_headers
		for i in range(0, number_of_headers):
			
			# search for elem_start in lines_source
			for element in lines_source:
				if elem_start not in element:
					header_id_start = -1
					continue
				else:
					# if first line in a header contains either "* Auxiliary piece of code" or "Class Attributes" string - skip this header
					# if the above - do_execute = False
					if (elem_skip_header_1 in lines_source[lines_source.index(element)+1] or elem_skip_header_2 in lines_source[lines_source.index(element)+1]):
						header_id_skip = lines_source.index(element)+1
						number_of_headers = number_of_headers - 1
						del lines_source[0: header_id_skip]
						#print 'number_of_headers:' + str(number_of_headers)
						do_execute = False
						break
					# if first line in a header contains "Method Name" - copy this line ID and process the header further
					# if the above - do_execute = True
					if elem_ok_header_code in lines_source[lines_source.index(element)+1]:
						header_id_start = lines_source.index(element)
						do_execute = True
						break
			
			# if elem_start found, search for elem_stop in lines_source
			if(do_execute):
				for element in lines_source:
					if elem_stop not in element:
						header_id_stop = -1
						continue
					else:
						header_id_stop = lines_source.index(element)
						break
				
				# process if header_id_start and header_id_stop found for a header that defines a method
				if (( header_id_start != -1 ) and ( header_id_stop != -1 )):
				
					# do not process elem_start and elem_stop, hence lines_source[header_id_start+1: header_id_stop] instead of [header_id_start: header_id_stop+1]
					lines_header = lines_source[header_id_start+1: header_id_stop]
					lines_header_concatenated_string = concatenate_list_data(lines_header)
					lines_header_concatenated_list =  lines_header_concatenated_string.split('*')
					# delete first index that is empty as a result of the above split operation
					del lines_header_concatenated_list[0]
					
					# do not process if the length of lines_header_concatenated_list equals ZERO
					if (len(lines_header_concatenated_list) != 0):
						isfirstline = True
						method_header_list = []
						#build method_header_list
						for line in lines_header_concatenated_list:
							if len(line) > 2:
								# append number of line from the source file
								if(isfirstline):
									method_header_list.append(source_file)
									index_of_line_in_header = lines_header_concatenated_list.index(line)
									orginal_line = lines_header[index_of_line_in_header]
									index_of_line_in_source = lines_source_fixed.index(orginal_line)
									method_header_list.append(index_of_line_in_source)
									isfirstline = False
								line = line.replace("\t", "")
								method_header_list.append(line[1:])
					
						resulted_array_of_method_headers[iterator] = method_header_list
						iterator = iterator + 1
					
					# delete lines from the lines_source list that have been already processed
					del lines_source[0: header_id_stop+1]
		
	return resulted_array_of_method_headers