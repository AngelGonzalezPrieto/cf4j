package es.upm.etsisi.cf4j.qualityMeasure.prediction;

import es.upm.etsisi.cf4j.data.TestUser;
import es.upm.etsisi.cf4j.qualityMeasure.QualityMeasure;
import es.upm.etsisi.cf4j.recommender.Recommender;

/**
 * <p>This class calculates the Coverage of the recommender system. The coverage is the capacity of
 * the recommender system to recommend new items. It is calculates as follows:</p>
 * <p>coverage = &lt;number of test items predicted&gt; / &lt;number of test items&gt;</p>
 * @author Fernando Ortega
 */
public class Coverage extends QualityMeasure {

	/**
	 * Constructor
	 * @param recommender Recommender instance for which the coverage are going to be computed
	 */
	public Coverage(Recommender recommender) {
		super(recommender);
	}

	@Override
	public double getScore(TestUser testUser, double[] predictions) {
		int count = 0;

		for (double prediction : predictions) {
			if (!Double.isNaN(prediction)) {
				count++;
			}
		}

		return (double) count / (double) testUser.getNumberOfTestRatings();
	}
}
