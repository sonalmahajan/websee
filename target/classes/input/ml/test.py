import sys
import operator

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
    score[key] = total[key]
    for i in range(len(positive[key])):
      score[key] *= vecter[i] * float(positive[key][i]) / p[key] + (1 - vecter[i]) * float(total[key] - positive[key][i]) / q[key]
  return score

def predict(feature):
  score = rank_bayes(feature)
  sorted_score = sorted(score.iteritems(), key=operator.itemgetter(1))
  return sorted_score

if __name__ == '__main__':
  # model_file = 'data3/train.model'
  # test_file = 'data2/train.model'
  model_file = sys.argv[1]
  test_file = sys.argv[2]
  load(model_file)
  rank = []
  with open(test_file) as f:
    for line in f:
      split = line.strip().split('\t')
      types = split[0]      
      property = split[1]
      vp = types + ":" + property
      feature = [float(x) for x in split[2:]]
      score = predict(feature)
      for i in range(len(score)):
        if vp == score[i][0]:
          # print property, len(score) - i
          rank.append(len(score) - i)
          break
  print rank
  print sum(rank) / float(len(rank)), len(total.keys())