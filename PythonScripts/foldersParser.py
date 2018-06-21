#!/usr/bin/env python

from __future__ import division
from SourceCode_files_parser import *
from UnitTests_files_parser import *
from IntegrationTests_files_parser import *
from ConstantsDefinition import *
from AuxiliaryScripts import *
import os
import urllib2
from unidiff import PatchSet

List_of_sourceFiles = list_files(dir_repository_master_sourceCode)
List_of_unitTests = list_files(dir_repository_master_unitTests)
List_of_integrationTests = list_files(dir_repository_master_integrationTests)


class Method(object):
	def __init__(self, name, in_class, description, affected_internal_variables, affected_external_variables, 
				called_internal_functions, called_external_functions, exceptions_thrown, exceptions_handled):
			self.name = name
			self.in_class = in_class
			self.description = description
			self.affected_internal_variables = affected_internal_variables
			self.affected_external_variables = affected_external_variables
			self.called_internal_functions = called_internal_functions
			self.called_external_functions = called_external_functions
			self.exceptions_thrown = exceptions_thrown
			self.exceptions_handled = exceptions_handled

SourceCode_files_List_toBeProcessed = SourceCode_files_parser(List_of_sourceFiles)
#UnitTest_files_List_toBeProcessed = UnitTests_files_parser(List_of_unitTests)
#IntegrationTest_files_List_toBeProcessed = IntegrationTests_files_parser(List_of_integrationTests)

for line in SourceCode_files_List_toBeProcessed:
	print line
'''
for line in UnitTest_files_List_toBeProcessed:
	print line
	
for line in IntegrationTest_files_List_toBeProcessed:
	print line
	
from unidiff import PatchSet
diff = urllib2.urlopen('https://github.com/matiasb/python-unidiff/pull/3.diff')
encoding = dir_diff_file2.headers.getparam('charset')
patch = PatchSet(dir_diff_file2, encoding=encoding)
print patch
'''