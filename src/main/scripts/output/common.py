'''
Created on Jan 28, 2014

@author: bailan
'''
import re

equal_dict = {
'dir' : [['', 'ltr']],
'background-attachment' : [['', 'scroll']],
'background-repeat' : [['', 'repeat']],
'border-bottom-style' : [['none', 'hidden']],
'border-top-style' : [['none', 'hidden']],
'border-left-style' : [['none', 'hidden']],
'border-style' : [['', 'none']],
'border-right-style' : [['none', 'hidden']],
'outline-style' : [['none', 'hidden']],
'font-style' : [['', 'normal']],
'font-family' : [['', 'times', 'courier', 'arial', 'serif', 'sans-serif', 'cursive', 'fantasy', 'monospace']],
'font-variant' : [['', 'normal']],
'font-weight' : [['', 'normal', '400'], ['bold', '700']],
'font-size' : [
  ['large', '18px', '13.5pt'],
  ['larger', '19px', '14pt'],
  ['medium', '16px', '12pt'],
  ['small', '13px', '10pt'],
  ['smaller', '13px', '10pt'],
  ['x-large', '24px', '18pt'],
  ['x-small', '10px', '7.5pt'],
  ['xx-large', '32px', '24pt'],
  ['xx-small', '9px', '7pt']],
'list-style-position' : [['', 'outside']],
'list-style-type' : [['', 'disc']],
'clear' : [['', 'none']],
'display' : [['', 'inline']],
'float' : [['', 'none']],
'overflow' : [['', 'visible']],
'position' : [['', 'static']],
'visibility' : [['', 'visible']],
'border-collapse' : [['', 'separate']],
'caption-side' : [['', 'top']],
'table-layout' : [['', 'auto']],
'direction' : [['', 'ltr']],
'text-align' : [['', 'left']],
'text-decoration' : [['', 'none']],
'text-indent' : [['', '0', '0px', '0pt']],
'text-transform' : [['', 'none']],
'white-space' : [['', 'normal']],
'border-width' : [['', 'medium']],
'border-bottom-width' : [['', 'medium']],
'border-top-width' : [['', 'medium']],
'border-left-width' : [['', 'medium']],
'border-right-width' : [['', 'medium']],
'outline-width' : [['', 'medium']],
'outline-bottom-width' : [['', 'medium']],
'outline-top-width' : [['', 'medium']],
'outline-left-width' : [['', 'medium']],
'outline-right-width' : [['', 'medium']],
'letter-spacing' : [['', 'normal']],
'line-height' : [['', 'normal']],
'vertical-align' : [['', 'baseline']],
'word-spacing' : [['', 'normal', '0', '0px']]}

def equal(expected_attribute, expected_value, actual_attribute, actual_value):
  global equal_dict
  if expected_attribute != actual_attribute:
    return False
  if expected_value == actual_value:
      return True
  if expected_attribute not in equal_dict:
    return False
  for e in equal_dict[expected_attribute]:
    if expected_value in e and actual_value in e:
      return True
  return False

def list_equal(expected_attribute, expected_value, actual_attributes, actual_values):
  global equal_dict
  for i in range(len(actual_attributes)):
    if expected_attribute != actual_attributes[i]:
      continue
    if expected_value == actual_values[i]:
      return True
    if expected_attribute not in equal_dict:
      return False
    for e in equal_dict[expected_attribute]:
      if expected_value in e and actual_values[i] in e:
        return True
  return False

def patch_xpath(xpath):
  return re.sub(r'(/table(\[\d+\])?)', r'\1/tbody[1]', xpath)

def xpath_equal(xpath1, xpath2):
  if not xpath1:
    return False
  if not xpath2:
    return False
  xpath1 = re.sub(r'(/tbody(\[\d+\])?)', r'', xpath1)
  xpath2 = re.sub(r'(/tbody(\[\d+\])?)', r'', xpath2)
  if xpath1 == xpath2:
    return True
  if xpath1.replace('[1]', '') == xpath2.replace('[1]', ''):
    return True
  return False
