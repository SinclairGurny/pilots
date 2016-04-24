import UnitConverter as uc
import DataBase as db
import LearnModel as lm
import matplotlib.pyplot as plt
import numpy as np
import math

# we only need part of the data to train the model
# currently a place holder for more general interface
# only for test and fun ... 
def getCruisePhase(database):
	# we define cruise phase as acceleration = 0 +- 0.01 ( about 500 N) and speed >= 10m/s
	speed = database.select("true air speed")
	time = database.select("_zulu,_time")
	# calculate acceleration
	a = [0]*(len(speed) - 1)
	result = []
	start = -1
	end = -1
	for i in xrange(len(a)):
		a[i] = (speed[i+1] - speed[i])/(time[i+1] - time[i])
		if (abs(a[i]) < 100 and speed[i] > 10):
			if start != -1:
				end += 1
			else:
				start = i
				end = i+1
		else:
			if start != -1:
				result.append((start, end))
				start = -1
				end = -1
	if start != -1:
		result.append((start, end))
	r = filter(lambda(t): t[1] - t[0] > 200, result)
	return r

class LeastSquare(lm.SupervisedLearningModel):
	def _error(self,x,y,param):
		r = y - np.dot(x,param)
		se = np.dot(r.transpose(), r)
		rmse = math.sqrt(se/x.shape[0])
		return {'RMSE': rmse, 'SE': se}
	def _train(self,x,y):
		print np.linalg.cond(x)
		q, r = np.linalg.qr(x)
		b = np.dot(q.transpose(),y)
		weight = np.linalg.lstsq(r[:3,:3],b[:3])[0]
		#weight = np.linalg.lstsq(np.dot(x.transpose(), x), np.dot(x.transpose(),y))[0]
		return weight
	def _eval(self, x, param):
		return np.dot(x, param)

# special case of the linear transformation ( for this test... )
class SpecialLinearTransformation(lm.DataTransformer):
	def _YTrans(self, Y):
		r = np.array([Y[:,:].sum(axis=1) + 8506*4.448221628254617]).transpose()
		return r
	def _XTrans(self, X):
		# place holder
		x1 = np.divide(np.multiply(np.power(X[:,0],2), X[:,1]),X[:,2]);
		x2 = np.multiply(x1,X[:,3])
		x3 = np.multiply(x2, X[:,3])
		A = np.ones((X.shape[0],3))
		A[:,1] = x1
		A[:,2] = x2
		return A
if __name__ == '__main__':
	converter = uc.UnitConverter('unit.json')
	data = db.DataBase('initConfig.json', True)
	data.addData(db.DataParser('data/High Power King Air.txt', '|'))
	data.addData(db.DataParser('data/Medium Power King Air.txt', '|'))
	data.loadConfiguration('afterConfig.json', converter)
	XF = ['true air speed', 'ambient pressure', 'ambient temperature','angle of attack']
	YF = [ 'fuel 1','fuel 2', 'fuel 3','fuel 4', 'fuel 5','fuel 6','fuel 7','fuel 8' ]
	trans = SpecialLinearTransformation(XF, YF)
	model = LeastSquare(trans)
	resultRange = getCruisePhase(data)
	print resultRange
	for i in resultRange:
		model.readData(data, i[0],i[1])
	print model.train()
	print model.inSampleError(False)
	testData = db.DataBase('initConfig.json',True)
	testData.addData(db.DataParser('data/Low Power King Air.txt', '|'))
	testData.loadConfiguration('afterConfig.json', converter)
	print model.outSampleError(testData, 0, 3000, True)
	l = data.select('true air speed')
	viewer = db.DataBaseViewer(testData)
	viewer.visualize()
