import java.util.Scanner;
import moa.streams.ArffFileStream;

public class ConfigureStream {
    private static ArffFileStream selectedStream;
    private int totalInstances=40000;
    private int batchSize = 1000;
    ConfigureStream(){}
    public void displayMenu(){
        boolean exit=false;
        Scanner userIn = new Scanner(System.in);
        System.out.println("Please enter a menu option:");
        System.out.print("1) Select Stream");
        if(selectedStream!=null){
            System.out.print(": " + selectedStream.arffFileOption.getFile().getName() + "\n");
        } else {
            System.out.print("\n");
        }
        System.out.println(
                "2) Set Batch Size: " + batchSize + "\n" +
                "3) Set Total Instances to Stream: " + this.totalInstances + "\n" +
                "101) Go Back");
        try {
            int userChoice = userIn.nextInt();
            switch (userChoice){
                case 1:
                    selectedStream = setStream(userIn);
                    break;
                case 2:
                    System.out.println("Please enter the new Batch Size:");
                    try {
                        batchSize = userIn.nextInt();
                    } catch (Exception e){
                        System.out.println("\nPlease Input a Valid Number!\n");
                    }
                    break;
                case 3:
                    System.out.println("Please enter the number of Instances to Stream:");
                    try {
                        totalInstances = userIn.nextInt();
                    } catch (Exception e){
                        System.out.println("\nPlease Input a Valid Number!\n");
                    }
                    break;
                case 101:
                    exit=true;
                default: break;
            }
        } catch (Exception e){
            System.out.println("\nPlease Input a Number!\n");
        }
        if(!exit){
            this.displayMenu();
        }
    }
    private ArffFileStream setStream(Scanner userIn){
        System.out.println("Please Enter the Name of the File to Configure a Stream For: ");
        String fileName = userIn.next();
        System.out.println("Please Enter the Class Index");
        int cIdx = userIn.nextInt();
        try {
            return new ArffFileStream(fileName, cIdx);
        } catch(Exception e) {
            System.out.println("\nFile Not Found! (or invalid index)\n");
            return null;
        }
    }
    public int getBatchSize(){
        return batchSize;
    }
    public int getTotalInstances(){
        return totalInstances;
    }
    public ArffFileStream getSelectedStream(){
        return selectedStream;
    }
}