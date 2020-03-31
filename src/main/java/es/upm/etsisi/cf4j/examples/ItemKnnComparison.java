package es.upm.etsisi.cf4j.examples;

import es.upm.etsisi.cf4j.data.DataModel;
import es.upm.etsisi.cf4j.data.DataSet;
import es.upm.etsisi.cf4j.data.RandomSplitDataSet;
import es.upm.etsisi.cf4j.qualityMeasure.QualityMeasure;
import es.upm.etsisi.cf4j.qualityMeasure.prediction.Coverage;
import es.upm.etsisi.cf4j.qualityMeasure.prediction.MAE;
import es.upm.etsisi.cf4j.qualityMeasure.prediction.MSLE;
import es.upm.etsisi.cf4j.qualityMeasure.recommendation.NDCG;
import es.upm.etsisi.cf4j.qualityMeasure.recommendation.Precision;
import es.upm.etsisi.cf4j.qualityMeasure.recommendation.Recall;
import es.upm.etsisi.cf4j.recommender.Recommender;
import es.upm.etsisi.cf4j.recommender.knn.ItemKNN;
import es.upm.etsisi.cf4j.recommender.knn.itemSimilarityMetric.*;
import es.upm.etsisi.cf4j.util.PrintableQualityMeasure;
import es.upm.etsisi.cf4j.util.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * In this example we compare the MSLE and nDCG quality measures scores for different similarity metrics applied to
 * item-to-item knn based collaborative filtering. Each similarity metric is tested with different number of neighbors.
 */
public class ItemKnnComparison {

	// Grid search over number of neighbors hyper-parameter
	private static final int[] numNeighbors = Range.ofIntegers(100,50,5);

	// Fixed aggregation approach
	private static final ItemKNN.AggregationApproach aggregationApproach = ItemKNN.AggregationApproach.MEAN;

	// Random seed to guaranty reproducibility of the experiment
	private static final long randomSeed = 43;

	public static void main (String [] args) {

		// Load MovieLens 100K dataset
    	DataSet ml1m = new RandomSplitDataSet("src/main/resources/datasets/ml100k.data", 0.2, 0.2, "\t", randomSeed);
		DataModel datamodel = new DataModel(ml1m);

		// Dataset parameters
		double median = 3;
		double[] relevantRatings = {3, 4, 5};
		double[] notRelevantRatings = {1, 2};

		// To store results
		PrintableQualityMeasure msleScores = new PrintableQualityMeasure("MSLE", numNeighbors);
		PrintableQualityMeasure ndcgScores = new PrintableQualityMeasure("NDCG", numNeighbors);

		// Create similarity metrics
		List<ItemSimilarityMetric> metrics = new ArrayList<>();
		metrics.add(new AdjustedCosine());
		metrics.add(new Correlation());
		metrics.add(new CorrelationConstrained(median));
		metrics.add(new Cosine());
		metrics.add(new Jaccard());
		metrics.add(new JMSD());
		metrics.add(new MSD());
		metrics.add(new PIP());
		metrics.add(new Singularities(relevantRatings, notRelevantRatings));
		metrics.add(new SpearmanRank());

		// Evaluate ItemKNN recommender
		for (ItemSimilarityMetric metric : metrics) {
			String metricName = metric.getClass().getSimpleName();

			for (int k : numNeighbors) {
				Recommender knn = new ItemKNN(datamodel, k, metric, aggregationApproach);
				knn.fit();

				QualityMeasure msle = new MSLE(knn);
				msleScores.putScore(k, metricName, msle.getScore());

				QualityMeasure ndcg = new NDCG(knn,10);
				ndcgScores.putScore(k, metricName, ndcg.getScore());
			}
		}

		// Print results
		msleScores.print();
		ndcgScores.print();
	}
}