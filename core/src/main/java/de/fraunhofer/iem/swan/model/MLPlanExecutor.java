package de.fraunhofer.iem.swan.model;

import ai.libs.jaicore.ml.classification.loss.dataset.EClassificationPerformanceMeasure;
import ai.libs.jaicore.ml.classification.singlelabel.SingleLabelClassification;
import ai.libs.jaicore.ml.core.dataset.schema.attribute.IntBasedCategoricalAttribute;
import ai.libs.jaicore.ml.core.dataset.serialization.ArffDatasetAdapter;
import ai.libs.jaicore.ml.core.evaluation.evaluator.SupervisedLearnerExecutor;
import ai.libs.jaicore.ml.core.filter.SplitterUtil;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import ai.libs.mlplan.core.MLPlan;
import ai.libs.mlplan.multiclass.wekamlplan.MLPlanWekaBuilder;
import de.fraunhofer.iem.swan.features.InstancesHandler;
import de.fraunhofer.iem.swan.util.Util;
import org.api4.java.ai.ml.classification.singlelabel.evaluation.ISingleLabelClassification;
import org.api4.java.ai.ml.core.dataset.schema.attribute.IAttribute;
import org.api4.java.ai.ml.core.dataset.serialization.DatasetDeserializationFailedException;
import org.api4.java.ai.ml.core.dataset.splitter.SplitFailedException;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.evaluation.execution.ILearnerRunReport;
import org.api4.java.ai.ml.core.evaluation.execution.LearnerExecutionFailedException;
import org.api4.java.ai.ml.core.exception.TrainingException;
import org.api4.java.algorithm.Timeout;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.nd4j.common.io.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Oshando Johnson on 27.09.20
 */
public class MLPlanExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MLPlanExecutor.class);
    private final int ITERATIONS = 1;

    public MLPlanExecutor() {

    }

    /**
     * Run ML-Plan using the provided path to the ARFF file.
     *
     * @param instancesHandler file path for ARFF file
     */
    public HashMap<String, String> evaluateDataset(InstancesHandler instancesHandler) {

        String arffFilePath = Util.exportInstancesToArff(instancesHandler.getInstances());

        String mClass = Util.getClassName(instancesHandler.getInstances());

        long start = System.currentTimeMillis();

        //Initialize dataset using ARFF file path
        ILabeledDataset<?> dataset = null;
        try {
            dataset = ArffDatasetAdapter.readDataset(new File(arffFilePath));
        } catch (DatasetDeserializationFailedException e) {
            e.printStackTrace();
        }

        //dataset.removeColumn("id");

        ArrayList<Double> fScores = new ArrayList<>();
        ArrayList<String> algorithms = new ArrayList<>();

        ModelEvaluator modelEvaluator = new ModelEvaluator();

        ArffLoader loader = new ArffLoader();
        try {
            loader.setFile(new File(arffFilePath));
            Instances instances = loader.getDataSet();
            instances.setClassIndex(instances.numAttributes() - 1);
            modelEvaluator.initializeResultSet(instances);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //For each iteration, create a new train-test-split and run ML-Plan
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {

            System.out.println("Iteration #"+iteration);
            try {
                List<ILabeledDataset<?>> split = SplitterUtil.getLabelStratifiedTrainTestSplit(dataset, new Random(1337 + (iteration * 11)), 0.7);
                LOGGER.info("Data read. Time to create dataset object was {}ms", System.currentTimeMillis() - start);

                Classifier optimizedClassifier = getClassifier(split.get(0));
                //System.out.println("Classify: " + optimizedClassifier.getClassifier().getClass().getSimpleName());

                //optimizedClassifier.fit(split.get(0));


                String trainPath = "/Users/oshando/Projects/thesis/03-code/swan/swan_core/swan-out/mlplan/train-methods-dataset.arff";
                ArffDatasetAdapter.serializeDataset(new File(trainPath), split.get(0));
                ArffLoader trainLoader = new ArffLoader();
                trainLoader.setFile(new File(trainPath));
                Instances trainInstances = trainLoader.getDataSet();
                trainInstances.setClassIndex(trainInstances.numAttributes() - 1);


                String testPath = "/Users/oshando/Projects/thesis/03-code/swan/swan_core/swan-out/mlplan/test-methods-dataset.arff";
                ArffDatasetAdapter.serializeDataset(new File(testPath), split.get(1));
                ArffLoader testLoader = new ArffLoader();
                testLoader.setFile(new File(testPath));
                Instances testInstances = testLoader.getDataSet();
                testInstances.setClassIndex(testInstances.numAttributes() - 1);

                modelEvaluator.evaluate(optimizedClassifier, trainInstances, testInstances, iteration);


                /* evaluate solution produced by mlplan */
        /*       SupervisedLearnerExecutor executor = new SupervisedLearnerExecutor();
               ILearnerRunReport report = executor.execute(optimizedClassifier, split.get(0), split.get(1));

                for (Object pred : report.getPredictionDiffList().getPredictionsAsList()) {

                    SingleLabelClassification cl = (SingleLabelClassification) pred;

                }

                for (Object prediction : report.getPredictionDiffList().getPredictionsAsList()) {

                    SingleLabelClassification label = (SingleLabelClassification) prediction;
                }




                LOGGER.info("Model selected: {},{},{},{}", mClass, iteration,
                        optimizedClassifier.getClassifier().getClass().getSimpleName(),
                        EClassificationPerformanceMeasure.F1_WITH_1_POSITIVE.F1_WITH_1_POSITIVE.loss(report.getPredictionDiffList().getCastedView(Integer.class, ISingleLabelClassification.class)));
*/
                //fScores.add(EClassificationPerformanceMeasure.F1_WITH_1_POSITIVE.loss(report.getPredictionDiffList().getCastedView(Integer.class, ISingleLabelClassification.class)));
                //algorithms.add(optimizedClassifier.getClassifier().getClass().getSimpleName());
                //LOGGER.info("Error Rate of the solution produced by ML-Plan: {}. ",  );

            } catch (SplitFailedException | InterruptedException | IOException  e) {
                e.printStackTrace();
            }
        }
        return modelEvaluator.getFMeasure();
    }

    public void evaluateDataset(InstancesHandler instancesHandler, int k) {

        //arffFilePath = "/Users/oshando/Projects/thesis/03-code/swan/swan_core/src/main/resources/waveform.arff";
        String arffFilePath = Util.exportInstancesToArff(instancesHandler.getInstances());

        String mClass = Util.getClassName(instancesHandler.getInstances());


        long start = System.currentTimeMillis();

        //Initialize dataset using ARFF file path
        ILabeledDataset<?> dataset = null;
        try {
            dataset = ArffDatasetAdapter.readDataset(new File(arffFilePath));
        } catch (DatasetDeserializationFailedException e) {
            e.printStackTrace();
        }

        //dataset.removeColumn("id");

        ModelEvaluator modelEvaluator = new ModelEvaluator();

        //For each iteration, create a new train-test-split and run ML-Plan
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {


            try {
                List<ILabeledDataset<?>> split = SplitterUtil.getLabelStratifiedTrainTestSplit(dataset, new Random(1337 + (iteration * 11)), 0.7);
                LOGGER.info("Data read. Time to create dataset object was {}ms", System.currentTimeMillis() - start);


                System.out.println(split.get(1).getLabelAttribute().getName());
                for (IAttribute attribute : split.get(1).getListOfAttributes()) {

                    //   System.out.println(attribute.getName());
                }
                ArffDatasetAdapter.serializeDataset(new File("/Users/oshando/Projects/thesis/03-code/swan/swan_core/swan-out/mlplan/methods-dataset.arff"), split.get(1));


                for (int x = 0; x < split.get(1).size(); x++) {

                    int attributeIndex = split.get(1).getNumAttributes() - 1;
                    //System.out.println(Arrays.toString(split.get(1).get(x).getAttributes()));

                    IAttribute attribute = split.get(1).getAttribute(attributeIndex);

                    //System.out.println(dataset.getLabelVector().);
                    System.out.println(((IntBasedCategoricalAttribute) split.get(1).getAttribute(attributeIndex)).getLabelOfCategory((int) split.get(1).get(x).getAttributeValue(attributeIndex)));


                    System.out.println(((IntBasedCategoricalAttribute) split.get(1).getLabelAttribute()).getLabelOfCategory((int) split.get(1).get(x).getLabel()));


                    // System.out.println(split.get(1).getAttribute());

                    System.out.println(split.get(1).get(x).getAttributeValue(split.get(1).getNumAttributes() - 2) + "   " + split.get(1).get(x).getAttributeValue(split.get(1).getNumAttributes() - 1));
                }


            } catch (SplitFailedException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * SReturns trained clssifier
     *
     * @param trainingSet training set
     * @return trained classifier
     */
    public Classifier getClassifier(ILabeledDataset<?> trainingSet) {

        Classifier optimizedClassifier = null;
        /* initialize mlplan with a tiny search space, and let it run for 30 seconds */

        try {
            MLPlan<IWekaClassifier> mlPlan = new MLPlanWekaBuilder()
                    .withNumCpus(12)//Set to about 12 on the server
                    .withSeed(35467463)
                    //set default timeout
                    .withTimeOut(new Timeout(30, TimeUnit.SECONDS))
                    .withDataset(trainingSet)
                    .withCandidateEvaluationTimeOut(new Timeout(30, TimeUnit.SECONDS))
                    .withPortionOfDataReservedForSelection(0.0)//ignore selection phase
                    .withPerformanceMeasureForSearchPhase(EClassificationPerformanceMeasure.F1_WITH_1_POSITIVE)//use F1
                    .withMCCVBasedCandidateEvaluationInSearchPhase(5, .7)
                    .build();

            mlPlan.setLoggerName("testedalgorithm");

            long start = System.currentTimeMillis();

            optimizedClassifier = mlPlan.call().getClassifier();

            long trainTime = (int) (System.currentTimeMillis() - start) / 1000;
            LOGGER.info("Finished build of the classifier. Training time was {}s.", trainTime);
            LOGGER.info("Internally believed error was {}", mlPlan.getInternalValidationErrorOfSelectedClassifier());

        } catch (IOException | AlgorithmTimeoutedException | InterruptedException | AlgorithmException | AlgorithmExecutionCanceledException e) {
            e.printStackTrace();
        }
        return optimizedClassifier;
    }


    public static void maihn(String[] args) {

        String file = "/Users/oshando/Projects/thesis/03-code/swan/swan_core/src/main/resources/waveform.arff";

        MLPlanExecutor mlPlan = new MLPlanExecutor();
        //  mlPlan.evaluateDataset(file, "sdfs");
    }
}
