package es.upm.etsisi.cf4j.qualityMeasure.prediction;

import es.upm.etsisi.cf4j.data.TestUser;
import es.upm.etsisi.cf4j.qualityMeasure.QualityMeasure;
import es.upm.etsisi.cf4j.recommender.Recommender;

/**
 * This class calculates the Mean Squared Error (MSE) between the predictions and the test ratings.
 * @author Fernando Ortega
 */
public class MSE extends QualityMeasure {

	/**
	 * Constructor
	 * @param recommender Recommender instance for which the MSE are going to be computed
	 */
	public MSE(Recommender recommender) {
		super(recommender);
	}

	@Override
	public double getScore(TestUser testUser, double[] predictions) {

		double sum = 0d;
		int count = 0;
		
		for (int i = 0; i < testUser.getNumberOfTestRatings(); i++) {
			if (!Double.isNaN(predictions[i])) {
				double diff = predictions[i] -  testUser.getTestRatingAt(i);
				sum += diff * diff;
				count++;
			}
		}
		
		return (count == 0) ? Double.NaN : (sum / count);
	}
}