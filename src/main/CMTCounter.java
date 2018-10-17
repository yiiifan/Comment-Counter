package main;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CMTCounter {
    //total num of lines
    static int lines = 0;

    //total num of comment lines
    static int cmtLines = 0;

    //total num of single line comments
    static int singleLines = 0;

    //total num of block comments
    static int block = 0;

    //total num of lines within block comments
    static int blockLines = 0;

    //total num of TODO's
    static int todo = 0;

    static ArrayList<File> fileList = new ArrayList<File>();

    public static void main(String[] args) throws IOException {

        //get the folder path from console
        System.out.println("Enter the folder path: ");
        //String folderpath = "C:\\Users\\sdall\\Desktop\\testcode";
        Scanner scanner = new Scanner(System.in);
        String folderpath = scanner.nextLine();

        File file = new File(folderpath);
        scanFolder(file);

        for(File f : fileList){

            //determine the file type
            String filename = f.getName().toString();
            String fileType = filename.substring(filename.indexOf('.')+1);

            //count the total number of lines
            countLines(f);

            //calculate comment lines
            countFile(f, fileType);

            //print out the outcome
            System.out.println("\nThe summary of "+filename+" is:");
            System.out.println("Total # of lines: "+ lines);
            System.out.println("Total # of cmtLines: "+ cmtLines);
            System.out.println("Total # of singleLines: "+ singleLines);
            System.out.println("Total # of block: "+ block);
            System.out.println("Total # of blockLines: "+ blockLines);
            System.out.println("Total # of todo: "+ todo);

            reset();

        }
    }

    private static void countLines(File f) throws IOException{
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String line = null;

        while((line = br.readLine()) != null){
            lines++;
        }
    }

    //counting lines according to different file types
    private static void countFile(File f, String fileType) throws IOException{
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String line = null;

        /** The type of file is java or javascript and the comment styles are:
         single comment: single line start with "//",
         block comment: continuous multiple lines start with "/*" end with "*\/"
         */
        if(fileType.equals("java") || fileType.equals("js")){

            while((line = br.readLine()) != null){
                line = line.trim();
                countSingle(line, br, "//");
                br = countBlock(line, br, "/*", "*/");
            }

            //add up single comment lines and block comment lines to total comment lines
            cmtLines = singleLines + blockLines;
        }

        /** The type of file is python and the comment style of python are:
         single comment: single line start with "#",
         block comment: continuous multiple lines start with Hashtag(#) or
         continuous multiple lines both start and end with three quotation marks('''/""")
         */
        else if(fileType.equals("py")){

            //inBlock means the previous line is in a comment block
            boolean inBlock = false;
            //firstCmtLine means the previous line is a comment line not included in any block comment
            boolean firstCmtLine = false;

            while((line = br.readLine()) != null){
                br = countBlock(line, br, "\"\"\"", "\"\"\"");
                br = countBlock(line, br, "\'\'\'", "\'\'\'");

                if(line.startsWith("#")){
                    //the current line is a comment line start with "#"
                    todo  += countTODO(line);
                    if(inBlock){
                        //the previous line is already in a comment block, so just add up blockLines
                        blockLines++;
                    }else if(firstCmtLine){
                        //the previous line is a comment line but not included in a block, so come up with a new block by combining current line with the previous one
                        block++;
                        blockLines += 2;
                        inBlock = true;
                        firstCmtLine = false;
                    }else{
                        //the previous line isn't a comment line
                        firstCmtLine = true;
                    }
                }else if(line.contains("#")){
                    //count the single comment at the end of line
                    singleLines++;
                    int index = line.indexOf("#");
                    String subline = line.substring(index);
                    todo += countTODO(subline);
                }else{
                    //current line isn't a comment line while the previous line is, the previous line is a single comment line
                    if(firstCmtLine){
                        singleLines++;
                        firstCmtLine = false;
                    }
                    inBlock = false;
                }
            }
            //add up single comment lines and block comment lines to total comment lines
            cmtLines = singleLines + blockLines;
        }

        else{
            System.out.println("\nPlease specify the comment style of "+ fileType+":");
            System.out.println("Please enter the single comment pattern(Press Enter if not exists): ");
            Scanner s1 = new Scanner(System.in);
            String singlePattern = s1.nextLine();
            System.out.println("Please enter the start of block comment pattern(Press Enter if not exists): ");
            Scanner s2 = new Scanner(System.in);
            String blockStartPattern = s1.nextLine();
            System.out.println("Please enter the end of block comment pattern(Press Enter if not exists): ");
            Scanner s3 = new Scanner(System.in);
            String blockEndPattern = s3.nextLine();

            while((line = br.readLine()) != null){
                line = line.trim();
                if(singlePattern.length()!=0){
                    countSingle(line, br, singlePattern);
                }
                if(blockEndPattern.length()!=0 && blockEndPattern.length()!=0){
                    br = countBlock(line, br, blockStartPattern, blockEndPattern);
                }
            }
            //add up single comment lines and block comment lines to total comment lines
            cmtLines = singleLines + blockLines;
        }


    }

    // count the # of TODO's in comment lines
    private static int countTODO(String line){
        int cntTODO = 0;
        //specify the pattern which could occur in the comment several times
        Matcher m = Pattern.compile("(?=(TODO))").matcher(line);
        while(m.find()){
            cntTODO++;
        }
        return  cntTODO;

    }

    //count the # of single comments in the file
    /* Two types of single comments:
    *   1. the line start with specific pattern,
    *   2. the coding line is combined with a comment line at the end.
    *   In the second situation, we suppose that there is no pattern in the range of quote,
    *   for example: int String = "//my test";
    *   it will be still detected as a single comment
    *   */
    private  static void countSingle(String line, BufferedReader br, String start) throws IOException{
        if(line.startsWith(start)){
            //count the line start with specific pattern
            singleLines++;
            todo  += countTODO(line);

        }else if(line.contains(start)) {
            //count the single comment at the end of line
            singleLines++;
            int index = line.indexOf(start);
            String subline = line.substring(index);
            todo += countTODO(subline);
        }

    }

    //count the # of block comments and lines within block comments
    private static BufferedReader countBlock(String line, BufferedReader br, String start, String end)throws IOException{
        if(line.startsWith(start)){
            block++;
            todo  += countTODO(line);

            while(!line.endsWith(end)){
                blockLines++;
                line = br.readLine().trim();
                todo  += countTODO(line);
            }
            blockLines++;
        }
        return br;

    }
    //iterate to add all files within the folder into fileList
    private static void scanFolder(File file){
        File[] files = file.listFiles();
        for(File f: files) {
            if (f.isDirectory()) {
                scanFolder(f);
            } else {
                String fileName = f.getName().toString();
                //ignore the filename starts with a "." and without an extension
                if(!(fileName.charAt(0) == '.' || fileName.indexOf(".") < 0)){
                    fileList.add(f);
                }
            }
        }
    }

    //reset all variable to initial value
    private static void reset(){
        lines = 0;
        cmtLines = 0;
        singleLines = 0;
        block = 0;
        blockLines = 0;
        todo = 0;
        fileList = new ArrayList<File>();
    }
}
