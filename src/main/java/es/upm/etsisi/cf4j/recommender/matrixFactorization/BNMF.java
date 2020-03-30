package es.upm.etsisi.cf4j.recommender.matrixFactorization;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import es.upm.etsisi.cf4j.data.DataModel;
import es.upm.etsisi.cf4j.data.Item;
import es.upm.etsisi.cf4j.util.Parallelizer;
import es.upm.etsisi.cf4j.util.Partible;
import es.upm.etsisi.cf4j.recommender.Recommender;
import es.upm.etsisi.cf4j.util.Maths;

import org.apache.commons.math3.special.Gamma;

/**
 * Implements Hernando, A., Bobadilla, J., &amp; Ortega, F. (2016). A non negative matrix factorization for
 * collaborative filtering recommender systems on a Bayesian probabilistic model. Knowledge-Based Systems, 97, 188-202.
 * @author Fernando Ortega
 */
public class BNMF extends Recommender {

	private final static double DEFAULT_R = 4;

	/**
	 * User factors
	 */
	private double[][] a;

	/**
	 * Item factors
	 */
	private double[][] b;

	/**
	 * Gamma parameters
	 */
	private double[][] gamma;

	/**
	 * Epsilon+ parameters
	 */
	private double[][] epsilonPlus;

	/**
	 * Epsilon- parameters
	 */
	private double[][] epsilonMinus;

	/**
	 * This hyper-parameter is related to the possibility of obtaining overlapping groups of users sharing the same
	 * tastes.
	 */
	private double alpha;

	/**
	 * This hyper-parameter represents the amount of evidence that the algorithm requires to deduce that a group of
	 * users likes an item.
	 */
	private double beta;

	/**
	 * Hyper-parameter of the binomial distribution.
	 */
	private double r;

	/**
	 * Number of factors
	 */
	private int numFactors;

	/**
	 * Number of iterations
	 */
	private int numIters;

	/**
	 * Model constructor
	 * @param datamodel DataModel instance
	 * @param numFactors Number of factors
	 * @param numIters Number of iterations
	 * @param alpha This parameter is related to the possibility of obtaining overlapping groups of users sharing the
	 *                 same tastes
	 * @param beta Amount of evidence that the algorithm requires to deduce that a group of users likes an item
	 */
	public BNMF(DataModel datamodel, int numFactors, int numIters, double alpha, double beta) {
		this(datamodel, numFactors, numIters, alpha, beta, DEFAULT_R, System.currentTimeMillis());
	}

	/**
	 * Model constructor
	 * @param datamodel DataModel instance
	 * @param numFactors Number of factors
	 * @param numIters Number of iterations
	 * @param alpha This parameter is related to the possibility of obtaining overlapping groups of users sharing the
	 *                 same tastes
	 * @param beta Amount of evidence that the algorithm requires to deduce that a group of users likes an item
	 * @param seed Seed for random numbers generation
	 */
	public BNMF(DataModel datamodel, int numFactors, int numIters, double alpha, double beta, long seed) {
		this(datamodel, numFactors, numIters, alpha, beta, DEFAULT_R, seed);
	}

	/**
	 * Model constructor
	 * @param datamodel DataModel instance
	 * @param numFactors Number of factors
	 * @param numIters Number of iterations
	 * @param alpha This parameter is related to the possibility of obtaining overlapping groups of users sharing the
	 *                 same tastes
	 * @param beta Amount of evidence that the algorithm requires to deduce that a group of users likes an item
	 * @param r Parameter of the binomial distribution (fixed to 4)
	 * @param seed Seed for random numbers generation
	 */
	public BNMF(DataModel datamodel, int numFactors, int numIters, double alpha, double beta, double r, long seed) {
		super(datamodel);

		this.numFactors = numFactors;
		this.numIters = numIters;
		this.alpha = alpha;
		this.beta = beta;
		this.r = r;

		Random rand = new Random(seed);

		// Users initialization
		this.gamma = new double[datamodel.getNumberOfUsers()][numFactors];
		for (int u = 0; u < datamodel.getNumberOfUsers(); u++) {
			for (int k = 0; k < numFactors; k++) {
				this.gamma[u][k] = rand.nextDouble();
			}
		}

		// Items initialization
		this.epsilonPlus = new double[datamodel.getNumberOfItems()][numFactors];
		this.epsilonMinus = new double[datamodel.getNumberOfItems()][numFactors];
		for (int i = 0; i < datamodel.getNumberOfItems(); i++) {
			for (int k = 0; k < numFactors; k++) {
				this.epsilonPlus[i][k] = rand.nextDouble();
				this.epsilonMinus[i][k] = rand.nextDouble();
			}
		}
	}

	/**
	 * Get the number of factors of the model
	 * @return Number of factors
	 */
	public int getNumFactors() {
		return this.numFactors;
	}

	/**
	 * Get the number of iterations
	 * @return Number of iterations
	 */
	public int getNumIters() {
		return this.numIters;
	}

	/**
	 * Get the gamma vector of an user
	 * @param userIndex user index
	 * @return User's gamma vector
	 */
	public double[] getGamma(int userIndex) {
		return this.gamma[userIndex];
	}

	/**
	 * Get the epsilon+ vector of an item
	 * @param itemIndex item index
	 * @return Item's epsilon+ vector
	 */
	public double[] getEpsilonPlus(int itemIndex) {
		return this.epsilonPlus[itemIndex];
	}

	/**
	 * Get the epsilon- vector of an item
	 * @param itemIndex item index
	 * @return Item's epsilon- vector
	 */
	public double[] getEpsilonMinus(int itemIndex) {
		return this.epsilonMinus[itemIndex];
	}

	@Override
	public void fit() {
		System.out.println("\nFitting BNMF...");

		for (int iter = 1; iter <= this.numIters; iter++) {

			Parallelizer.exec(datamodel.getItems(), new UpdateModel());

			if ((iter % 10) == 0) System.out.print(".");
			if ((iter % 100) == 0) System.out.println(iter + " iterations");
		}

		// set user factors
		this.a = new double[this.datamodel.getNumberOfUsers()][this.numFactors];
		for (int userIndex = 0; userIndex < this.datamodel.getNumberOfUsers(); userIndex++) {
			double sum = 0;
			for (int k = 0; k < this.numFactors; k++) {
				sum += this.gamma[userIndex][k];
			}

			for (int k = 0; k < this.numFactors; k++) {
				this.a[userIndex][k] = this.gamma[userIndex][k] / sum;
			}
		}

		// set item factors
		this.b = new double[this.datamodel.getNumberOfItems()][this.numFactors];
		for (int itemIndex = 0; itemIndex < this.datamodel.getNumberOfItems(); itemIndex++) {
			for (int k = 0; k < this.numFactors; k++) {
				this.b[itemIndex][k] = this.epsilonPlus[itemIndex][k] / (this.epsilonPlus[itemIndex][k] + this.epsilonMinus[itemIndex][k]);
			}
		}

	}

	@Override
	public double predict(int userIndex, int itemIndex) {
		double prob = Maths.dotProduct(this.a[userIndex], this.b[itemIndex]);
		prob = Math.max(prob, 1E-10);
		return Math.ceil(prob * (super.datamodel.getMaxRating() - super.datamodel.getMinRating() + 1));
	}

	/**
	 * Auxiliary inner class to parallelize model update
	 */
	private class UpdateModel implements Partible<Item> {

		private final static int NUM_LOCKS = 100;

		private ReentrantLock[] locks;

		private double[][] gamma;

		private double[][] epsilonPlus;

		private double[][] epsilonMinus;

		public UpdateModel() {

			// Locks avoid problem while users gammas are updated in different threads
			this.locks = new ReentrantLock [NUM_LOCKS];
			for (int i = 0; i < NUM_LOCKS; i++) {
				this.locks[i] = new ReentrantLock();
			}

			this.gamma = new double[datamodel.getNumberOfUsers()][numFactors];
			this.epsilonPlus = new double[datamodel.getNumberOfItems()][numFactors];
			this.epsilonMinus = new double[datamodel.getNumberOfItems()][numFactors];
		}

		@Override
		public void beforeRun() {
			// Init gamma
			for (double[] row : this.gamma) {
				Arrays.fill(row, alpha);
			}

			// Init epsilon+
			for (double[] row : this.epsilonPlus) {
				Arrays.fill(row, beta);
			}

			// Init epsilon-
			for (double[] row : this.epsilonMinus) {
				Arrays.fill(row, beta);
			}
		}

		@Override
		public void run(Item item) {
			int itemIndex = item.getItemIndex();

			for (int u = 0; u < item.getNumberOfRatings(); u++) {

				int userIndex = item.getUserAt(u);

				double [] lambda = new double [BNMF.this.numFactors];

				double rating = (item.getRatingAt(u) - datamodel.getMinRating()) / (datamodel.getMaxRating() - datamodel.getMinRating());

				double sum = 0;

				// Compute lambda
				for (int k = 0; k < BNMF.this.numFactors; k++) {
					lambda[k] = Math.exp(
						Gamma.digamma(BNMF.this.gamma[userIndex][k]) +
						BNMF.this.r * rating * Gamma.digamma(BNMF.this.epsilonPlus[itemIndex][k]) +
						BNMF.this.r * (1 - rating) * Gamma.digamma(BNMF.this.epsilonMinus[itemIndex][k]) -
						BNMF.this.r * Gamma.digamma(BNMF.this.epsilonPlus[itemIndex][k] + BNMF.this.epsilonMinus[itemIndex][k])
					);

					sum += lambda[k];
				}

				// Update model
				for (int k = 0; k < BNMF.this.numFactors; k++) {

					double l = lambda[k] / sum;

					// Update E+ & E-
					this.epsilonPlus[itemIndex][k] += l * BNMF.this.r * rating;
					this.epsilonMinus[itemIndex][k] += l * BNMF.this.r * (1 - rating);

					// Update gamma: user must be block to avoid concurrency problems
					int lockIndex = userIndex % this.locks.length;
					this.locks[lockIndex].lock();
					this.gamma[userIndex][k] += l;
					this.locks[lockIndex].unlock();
				}
			}
		}

		@Override
		public void afterRun() {
			BNMF.this.gamma = this.gamma;
			BNMF.this.epsilonPlus = this.epsilonPlus;
			BNMF.this.epsilonMinus = this.epsilonMinus;
		}
	}
}
