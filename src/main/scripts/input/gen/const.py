'''
Created on Jan 28, 2014

@author: bailan
'''

import sys

css = {
    'color' :
        ['background-color',
         'border-color',
         'border-bottom-color',
         'border-top-color',
         'border-left-color',
         'border-right-color',
         'outline-color',
         'color'
        ],
    'numeric' :
        ['height',
         'max-height',
        # 'max-width',
         'min-height',
        # 'min-width',
         'width',
         'counter-increment',
         'counter-reset',
         'margin',
         'margin-bottom',
         'margin-left',
         'margin-right',
         'margin-top',
         'padding',
         'padding-bottom',
         'padding-left',
         'padding-right',
         'padding-top',
         'bottom',
         'left',
         'top',
         'right',
         'z-index',
         'border-spacing',
         'text-indent',
         'background-position',
         'border-width',
         'border-bottom-width',
         'border-top-width',
         'border-left-width',
         'border-right-width',
         'outline-width',
         'font-size',
         'letter-spacing',
         'line-height',
         'vertical-align',
         'word-spacing'],
    'pre-defined' : 
        ['background-attachment',
         'background-repeat',
         'border-bottom-style',
         'border-top-style',
         'border-left-style',
         'border-right-style',
         'outline-style',
         'font-family',
         'font-style',
         'font-variant',
         'font-weight',
         'list-style-position',
         'list-style-type',
         'clear',
         'display',
         'float',
         'overflow',
         'position',
         'visibility',
         'border-collapse',
         'caption-side',
         'table-layout',
         'direction',
         'text-align',
         'text-decoration',
         'text-transform',
         'white-space']
}

css_predefined = {
    'background-attachment' : ['scroll', 'fixed' , 'local'],
    'background-repeat' : ['repeat', 'repeat-x', 'repeat-y', 'no-repeat'],
    'border-bottom-style' : ['none', 'hidden', 'dotted', 'dashed', 'solid' , 'double', 'groove' , 'ridge', 'inset', 'outset'],
    'border-top-style' : ['none', 'hidden', 'dotted', 'dashed', 'solid' , 'double', 'groove' , 'ridge', 'inset', 'outset'],
    'border-left-style' : ['none', 'hidden', 'dotted', 'dashed', 'solid' , 'double', 'groove' , 'ridge', 'inset', 'outset'],
    'border-right-style' : ['none', 'hidden', 'dotted', 'dashed', 'solid' , 'double', 'groove' , 'ridge', 'inset', 'outset'],
    'outline-style' : ['none', 'hidden', 'dotted', 'dashed', 'solid' , 'double', 'groove' , 'ridge', 'inset', 'outset'],
    'font-family' : ['times', 'courier', 'arial', 'serif', 'sans-serif', 'cursive', 'fantasy', 'monospace'],
    'font-style' : ['normal', 'italic', 'oblique'],
    'font-variant' : ['normal', 'small-caps'],
    'font-weight' : ['normal', 'bold', 'bolder', 'lighter', '100', '200', '300', '400', '500', '600', '700', '800', '900'],
    'list-style-position' : ['inside', 'outside'],
    'list-style-type' : ['armenian', 'circle', 'cjk-ideographic', 'decimal', 'decimal-leading-zero', 'disc', 'georgian', 'hebrew', 'hiragana', 'hiragana-iroha', 'katakana', 'katakana-iroha', 'lower-alpha', 'lower-greek', 'lower-latin', 'lower-roman', 'none', 'square', 'upper-alpha', 'upper-latin', 'upper-roman'],
    'clear' : ['left', 'right', 'both', 'none'],
    'display' : ['none', 'inline', 'block', 'inline-block', 'inline-table', 'list-item', 'run-in', 'table', 'table-caption', 'table-column-group', 'table-header-group', 'table-footer-group', 'table-row-group', 'table-cell', 'table-column', 'table-row', 'none'],
    'float' : ['left', 'right', 'none'],
    'overflow' : ['visible', 'hidden', 'scroll', 'auto'],
    'position' : ['static', 'absolute', 'fixed', 'relative'],
    'visibility' : ['visible', 'hidden', 'collapse'],
    'border-collapse' : ['collapse', 'separate'],
    'caption-side' : ['top', 'bottom'],
    'table-layout' : ['auto', 'fixed'],
    'direction' : ['ltr', 'rtl'],
    'text-align' : ['left', 'right', 'center', 'justify'],
    'text-decoration' : ['none', 'underline', 'overline', 'line-through'],
    'text-transform' : ['none', 'capitalize', 'uppercase', 'lowercase'],
    'white-space' : ['normal', 'nowrap', 'pre', 'pre-line', 'pre-wrap']
}


html = {
  'boolean' :
    ['autofocus',
     'checked',
     'hidden',
     'reversed',
     'selected'
    ],
  'numeric' :
    ['border',
     'cols',
     'colspan',
     'height',
     'rows',
     'rowspan',
     'size',
     'start',
     'value',
     'width'
    ],
  'pre-defined' : 
    ['dir',
     'type',
     'wrap'
    ]
}

html_predefined = {
  'dir' : ['ltr', 'rtl' , 'auto'],
  'type' : ['button', 'checkbox', 'file', 'hidden', 'image', 'password',  
    'radio', 'submit', 'text'],
  'wrap' : ['soft', 'hard']
}

html_elements = {
  'autofocus' : ['button', 'input', 'keygen', 'select', 'textarea'],
  'checked' : ['command', 'input'],
  'hidden' : [],
  'reversed' : ['ol'],
  'selected' : ['option'],
  'border' : ['img', 'object', 'table'],
  'cols' : ['textarea'],
  'colspan' : ['td', 'th'],
  'height' : ['canvas', 'embed', 'iframe', 'img', 'input', 'object', 'video'],
  'rows' : ['textarea'],
  'rowspan' : ['td', 'th'],
  'size' : ['input', 'select'],
  'start' : ['ol'],
  'value' : ['li'],
  'dir' : [],
  'type' : ['button', 'input', 'command', 'embed', 'object', 'script', 'source',
     'style', 'menu'],
  'width' : ['canvas','embed', 'iframe', 'img', 'input', 'object', 'video'],
  'wrap' : ['textarea']
}