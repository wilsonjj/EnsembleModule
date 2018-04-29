import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import moa.classifiers.Classifier;
import moa.streams.ArffFileStream;

import static java.lang.Double.NaN;

public class ModuleMenu {
    private static ConfigureStream configureStream = new ConfigureStream();
    private static EnsembleOptions ensembleOptions = new EnsembleOptions();
    private static ArrayList<Instance> randomChunk = new ArrayList<>();
    private static ArrayList<Double> ExperimentAverage = new ArrayList<>();
    private static Random rand = new Random();
    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        ModuleMenu mainMenu = new ModuleMenu();
        mainMenu.displayMenu();
    }
    public void displayMenu() throws IllegalAccessException, InstantiationException {
        boolean exit=false;
        Scanner userIn = new Scanner(System.in);
        System.out.println("Please enter a menu option: \n" +
                "1) Configure Stream \n" +
                "2) Ensemble Options \n" +
                "3) Execute \n"+
                "101) EXIT PROGRAM");
        //try {
            int userChoice = userIn.nextInt();
            switch (userChoice) {
                case 1:
                    configureStream.displayMenu();
                    break;
                case 2:
                    ensembleOptions.displayMenu();
                    break;
                case 3:
                    System.out.println("\nHow many times?");
                    int ExperimentNum = userIn.nextInt();
                    System.out.print("\n");
                    for (int i = 0;i<ExperimentNum;i++){
                        double instanceAverage = executeWAE(configureStream.getSelectedStream(),
                                configureStream.getTotalInstances(),
                                configureStream.getBatchSize(),
                                ensembleOptions.getEnsembleSize(),
                                ensembleOptions.getClassifier());
                        System.out.println("Experiment " + (i+1) + ": " + instanceAverage);
                        ExperimentAverage.add(instanceAverage);
                    }
                    double sum = 0;
                    for (int i = 0;i<ExperimentNum;i++){
                        sum += ExperimentAverage.get(i);
                    }
                    System.out.println("\nTask Complete\nAverage Accuracy: "+ sum/ExperimentNum + "\n");
                    break;
                case 101:
                    exit = true;
                    break;
                default:
                    break;
            }
        //} catch (Exception e){
            //System.out.println("\nPlease Input a Number!\n");
        //    System.out.println(e);
        //}
        if(!exit){
            this.displayMenu();
        }
    }
    private double executeWAE(ArffFileStream selectedStream, int totalInstances, int batchSize, int ensembleSize, Classifier classifier) throws IllegalAccessException, InstantiationException {
        int numberTested = 0;
        int numberTestedCorrect = 0;
        int numOfInstances = 0;
        selectedStream.prepareForUse();
        ArrayList<Classifier> ensemble = new ArrayList<>();
        ArrayList<Double> weights = new ArrayList<>();
        ArrayList<Integer> numIterations = new ArrayList<>();
        ArrayList<Integer> numCorrect = new ArrayList<>();
        ArrayList<Double> decisionWeights = new ArrayList<>();
        ArrayList<Double> decision = new ArrayList<>();
        while(numOfInstances + batchSize < totalInstances){
            ArrayList<Instance> chunk = makeChunk(selectedStream, batchSize);
            Classifier newClassifier = classifier.getClass().newInstance();
            newClassifier.setModelContext(selectedStream.getHeader());
            newClassifier.prepareForUse();
            //add random Instance to the random chunk
            if (randomChunk.size() < batchSize){
                randomChunk.add(chunk.get(rand.nextInt(batchSize)));
            } else {
                randomChunk.set(rand.nextInt(batchSize),chunk.get(rand.nextInt(batchSize)));
            }
            //test current ensemble
            if(ensemble.size() != 0){
                numberTested += batchSize;
                //increase iteration count (per batch, not per instance)
                for (int i = 0;i<ensemble.size();i++){
                    numIterations.set(i,numIterations.get(i)+1);
                }
                for (int i = 0;i<chunk.size();i++){
                    double finalDecision = NaN;
                    double finalDecisionWeight = 0.0;
                    //reset decision weights
                    for (int j = 0;j<decisionWeights.size();j++){
                        decisionWeights.set(j,0.0);
                    }
                    //calculate decision weights
                    for (int j = 0;j<ensemble.size();j++){
                        Prediction predict = ensemble.get(j).getPredictionForInstance(chunk.get(i));
                        double predictionClass = getPredictionClass(predict);
                        if (decision.indexOf(predictionClass) == -1){
                            decision.add(predictionClass);
                            decisionWeights.add(0.0);
                        }
                        int decisionIndex = decision.indexOf(predictionClass);
                        decisionWeights.set(decisionIndex,decisionWeights.get(decisionIndex)+weights.get(j));
                    }
                    for (int j = 0;j<decision.size();j++){
                        if (decisionWeights.get(j)>=finalDecisionWeight){
                            finalDecisionWeight = decisionWeights.get(j);
                            finalDecision = decision.get(j);
                        }
                    }
                    if (finalDecision == chunk.get(i).classValue()) {
                        numberTestedCorrect++;
                        for (int j = 0;j<ensemble.size();j++){
                            if (getPredictionClass(ensemble.get(j).getPredictionForInstance(chunk.get(i))) == finalDecision){
                                numCorrect.set(j, numCorrect.get(j) + 1);
                            }
                        }
                    }
                }
            }
            //train new classifier
            int learned = 0;
            while (learned < chunk.size()){
                newClassifier.trainOnInstance(chunk.get(learned));
                learned++;
            }
            //add to ensemble (maybe)
            if(ensemble.size() < ensembleSize){
                ensemble.add(newClassifier);
                weights.add(0.0);
                numCorrect.add(0);
                numIterations.add(1);
            } else {
                ArrayList<Classifier> ensembleCopy = (ArrayList<Classifier>)ensemble.clone();
                ArrayList<Classifier> tempEnsemble = new ArrayList<>();
                tempEnsemble.addAll(ensembleCopy);
                ensembleCopy.add(newClassifier);
                ArrayList<Classifier> ensembleRemoved = new ArrayList<>();
                for (int i = 0;i<ensembleCopy.size();i++){
                    ensembleRemoved.clear();
                    ensembleRemoved.addAll(ensembleCopy);
                    ensembleRemoved.remove(i);
                    double A = findGD(ensembleRemoved);
                    double B = findGD(tempEnsemble);
                    if (A>B){
                        tempEnsemble.clear();
                        tempEnsemble.addAll(ensembleRemoved);
                    }
                }
                //find the index of the removed classifier to remove from all arraylists
                Classifier removed = null;
                Classifier added = null;
                for (int i = 0;i<ensemble.size();i++){
                    int tempR = tempEnsemble.indexOf(ensemble.get(i));
                    if (tempR == -1){
                        removed = ensemble.get(i);
                    }
                    int tempA = ensemble.indexOf(tempEnsemble.get(i));
                    if (tempA == -1){
                        added = tempEnsemble.get(i);
                    }
                }
                //remove the removed classifier and corresponding stats
                if (removed != null){
                    int removeIndex = ensemble.indexOf(removed);
                    ensemble.remove(removeIndex);
                    weights.remove(removeIndex);
                    numCorrect.remove(removeIndex);
                    numIterations.remove(removeIndex);
                }
                //add new classifier and corresponding stats
                if (added != null){
                    ensemble.add(added);
                    weights.add(0.0);
                    numCorrect.add(0);
                    numIterations.add(0);
                }
            }
            //find ensemble weights
            double totalWeight = 0.0;
            for (int i = 0;i<ensemble.size();i++){
                totalWeight += findWeight(numCorrect.get(i),numIterations.get(i));
            }
            for (int i = 0;i<ensemble.size();i++){
                if (totalWeight == 0.0){
                    weights.set(i,0.0);
                } else {
                    weights.set(i, findWeight(numCorrect.get(i), numIterations.get(i)) / totalWeight);
                }
            }
            numOfInstances += batchSize;
        }
        return (double)numberTestedCorrect/(double)numberTested;
    }
    private ArrayList<Instance> makeChunk(ArffFileStream stream, int batchSize){
        ArrayList<Instance> chunk = new ArrayList<>();
        while (chunk.size() < batchSize){
            chunk.add(stream.nextInstance().getData());
        }
        return chunk;
    }
    private double findWeight(int numCorrect, int numIterations){
        double freq = (double)numCorrect / (double)numIterations;
        return freq / Math.sqrt((double)numIterations);
    }
    private double findGD(ArrayList<Classifier> currentEnsemble){
        double p1 = 0.0;
        double p2 = 0.0;
        for (int i = 1;i<=currentEnsemble.size();i++){
            double fp = failureProbability(currentEnsemble,i);
            p1 += (i*(i-1)*fp)/(double)(currentEnsemble.size()*(currentEnsemble.size()-1));
            p2 += (i*fp)/(double)currentEnsemble.size();
        }
        double p3 = 1-(p1/p2);
        if (Double.isNaN(p3)){
            p3 = 0;
        }
        return p3;
    }
    private double failureProbability(ArrayList<Classifier> currentEnsemble, int number){
        int fail = 0;
        int total = 0;
        ArrayList<Classifier> temp = new ArrayList<>();
        temp.addAll(currentEnsemble);
        ArrayList<Classifier> randomChosen = new ArrayList<>();
        Instance randomInstance = randomChunk.get(rand.nextInt(randomChunk.size()));
        for (int i = 0;i<number;i++){
            int tempIndex = rand.nextInt(temp.size());
            randomChosen.add(temp.get(tempIndex));
            temp.remove(tempIndex);
        }
        for (int i = 0;i<randomChosen.size();i++){
            total++;
            if (!randomChosen.get(i).correctlyClassifies(randomInstance)){
                fail++;
            }
        }
        return (double)fail/(double)total;
    }
    private double getPredictionClass(Prediction prediction){
        double[] instanceConfidence= prediction.getVotes(); //creates an array to store results of the model
        double predictedLabel= 0;
        double highest = instanceConfidence[0];
        for(int i=0; i<instanceConfidence.length;i++){
            if(instanceConfidence[i]>highest){
                highest=instanceConfidence[i];
                predictedLabel= i;
            }
        }
        return predictedLabel;
    }
}