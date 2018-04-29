import java.util.ArrayList;
import java.util.Scanner;
import com.github.javacliparser.Option;
import moa.classifiers.Classifier;

class EnsembleOptions {
    private static ArrayList <Classifier> classifiers = new ArrayList<>();
    private Classifier currentSelected;
    private int ensembleSize = 40;
    EnsembleOptions(){
        classifiers.add(new moa.classifiers.trees.HoeffdingAdaptiveTree());
        classifiers.add(new moa.classifiers.bayes.NaiveBayes());
        classifiers.add(new moa.classifiers.functions.Perceptron());
        currentSelected = classifiers.get(0);
    }
    public void displayMenu(){
        boolean exit=false;
        Scanner userIn = new Scanner(System.in);
        String currentSelectedString = currentSelected.getClass().toString().substring(22);
        int index = currentSelectedString.indexOf('.')+1;
        currentSelectedString = currentSelectedString.substring(index);
        System.out.println( "Please enter a menu option:\n" +
                            "1) Select Algorithm: " + currentSelectedString + "\n" +
                            "2) Edit Algorithm Parameters\n" +
                            "3) Set Ensemble Size: " + ensembleSize + "\n" +
                            "101) Go Back");
        try {
            int userChoice = userIn.nextInt();
            switch (userChoice){
                case 1:
                    selectAlgorithm(userIn,0);
                    break;
                case 2:
                    editParameters(userIn,0);
                    break;
                case 3:
                    System.out.println("Please enter Ensemble Size:");
                    try {
                        ensembleSize = userIn.nextInt();
                    } catch (Exception e){
                        System.out.println("\nPlease Input a Valid Number!\n");
                    }
                    break;
                case 101:
                    exit=true;
                    break;
                default: break;
            }
        } catch (Exception e){
            System.out.println("\nPlease Input a Number!\n");
        }
        if(!exit){
            displayMenu();
        }
    }
    private void selectAlgorithm(Scanner userIn, int selection){
        int selectionIdx=selection;
        boolean exit=false;

        for(int i=0; i<classifiers.size();i++){
            String classifierString = classifiers.get(i).getClass().toString().substring(22);
            int index = classifierString.indexOf('.')+1;
            classifierString = classifierString.substring(index);
            if(i==selectionIdx){
                System.out.println(classifierString +"  <<<");
            }
            else{
                System.out.println(classifierString);
            }
        }
        System.out.println("press 'e' to select or 'w'/'s' to scroll and \"exit\" to go back");
        String input= userIn.next();
        switch (input){
            case "e":
                currentSelected=classifiers.get(selectionIdx);
                exit=true;
                break;
            case "w":
                if(selectionIdx>0){
                    selectionIdx--;
                }
                break;
            case "s":
                if(selectionIdx<classifiers.size()-1){
                    selectionIdx++;
                }
                break;
            case "exit":
                exit=true;
                break;
        }
        if(!exit){
            selectAlgorithm(userIn,selectionIdx);
        }
    }
    private void editParameters(Scanner userIn, int selection){
        Option[] optionArr;
        optionArr = currentSelected.getOptions().getOptionArray();
        boolean exit = false;
        int selectionIdx= selection;

        for(int i=0; i<optionArr.length;i++){
            if(i==selectionIdx){
                System.out.println(optionArr[i].getName() + "  :   "+optionArr[i].getValueAsCLIString() +"  <<<");
            }
            else{
                System.out.println(optionArr[i].getName() + "  :   "+optionArr[i].getValueAsCLIString());
            }
        }
        System.out.println("press 'e' to select or 'w'/'s' to scroll and \"exit\" to go back");
        String input= userIn.next();
        switch (input){
            case "e":
                System.out.println("Please enter a value for  "+ optionArr[selectionIdx].getName());
                try {
                    optionArr[selectionIdx].setValueViaCLIString(userIn.next());
                } catch (Exception e){
                    System.out.println("\nInvalid Value!\n");
                }
                break;
            case "w":
                if(selectionIdx>0){
                    selectionIdx--;
                }
                break;
            case "s":
                if(selectionIdx<optionArr.length-1){
                    selectionIdx++;
                }
                break;
            case "exit":
                exit=true;
                break;
        }
        if(!exit){
            editParameters(userIn,selectionIdx);
        }
    }
    public Classifier getClassifier(){
        return currentSelected;
    }
    public int getEnsembleSize(){
        return ensembleSize;
    }
}