import operator

from feature import Feature

total = {}
positive = {}
  
def load(modelfile):
  global total
  global positive
  with open(modelfile) as f:
    for line in f:
      split = line.strip().split('\t')
      vp = split[0] + ":" + split[1]
      if vp in total:
        total[vp] += 1
      else:
        total[vp] = 1
      if vp in positive:
        for i in range(len(split[2:])):
          positive[vp][i] += int(split[2 + i])
      else:
        positive[vp] = [int(x) for x in split[2:]]  
  # print total
  # print positive

def predict(test_dir, xpath):
  feature = Feature(test_dir + '/oracle.png', test_dir + '/test.html', xpath)
  feature.process()
  vecter = feature.output_binary()
  print vecter
  score = rank_bayes(vecter)
  sorted_score = sorted(score.iteritems(), key=operator.itemgetter(1))
  return sorted_score
  
def rank_sqdiff(vecter):
  global total
  global positive
  score = {}
  for key in total.keys():
    score[key] = 0.0
    for i in range(len(positive[key])):
      score[key] += (1.0 - float(vecter[i]) - 
        float(positive[key][i]) / float(total[key])) ** 2
  return score

def rank(vecter):
  global total
  global positive
  score = {}
  for key in total.keys():
    score[key] = 1
    for i in range(len(positive[key])):
      score[key] *= vecter[i] * positive[key][i] + (1 - vecter[i]) * (total[key] - positive[key][i])
  return score

def rank_bayes(vecter):
  global total
  global positive
  p = {}
  q = {}
  for key in total.keys():
    p[key] = 0.0
    q[key] = 0.0
    for i in range(len(positive[key])):
      p[key] += float(positive[key][i])
      q[key] += float(total[key]) - float(positive[key][i])
  score = {}
  for key in total.keys():
    score[key] = 1
    for i in range(len(positive[key])):
      score[key] *= vecter[i] * float(positive[key][i]) / p[key] + (1 - vecter[i]) * float(total[key] - positive[key][i]) / q[key]
  return score

if __name__ == '__main__':
  home_dir = 'data2'
  load('mix.all')
  with open(home_dir + '/description.txt') as f:
    t = 0
    for line in f:
      t += 1
      split = line.strip().split('\t')
      property = split[2]
      types = split[3]
      xpath = split[7]
      testcase = split[8]
      score = predict(home_dir + '/' + testcase, xpath)
      print score
      vp = types + ":" + property
      break
      for i in range(len(score)):
        if vp == score[i][0]:
          print vp, len(score) - i
          break
