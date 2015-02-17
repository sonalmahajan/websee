import os
import timeit

from feature import Feature

if __name__ == '__main__':
  home_dir = 'data3'
  foutput = open('train.model', 'w')
  with open(home_dir + '/description.txt') as f:
    t = 0
    for line in f:
      t += 1
      split = line.strip().split('\t')
      start = timeit.default_timer()
      feature = Feature(home_dir + '/' + split[8] + '/oracle.png',
        home_dir + '/' + split[8] + '/test.html', split[7])
      feature.process()
      foutput.write(split[2])
      for item in feature.output().values():
        foutput.write('\t' + str(int(item == True)))
      foutput.write('\n')
      foutput.flush()
      stop = timeit.default_timer()
      print stop - start
      break
