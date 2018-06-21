#!/usr/bin/env python

from __future__ import division
import os
from ConstantsDefinition import *

# this function lists all files from subdirectories starting from a direstory given as the input argument
def list_files(dir):
    r = []
    for root, dirs, files in os.walk(dir):
        for name in files:
            r.append(os.path.join(root, name))
    return r

# this function concatenates list's elements in a signle string
def concatenate_list_data(list):
    result= ''
    for element in list:
        result += str(element)
    return result
	
# this function extracts filenames from path
def filename_fromPath(path):
    head, tail = os.path.split(path)
    return tail or os.path.basename(head)