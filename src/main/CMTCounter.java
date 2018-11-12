package main;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CMTCounter {
    // Total num of lines.
    private static int lines = 0;

    // Total num of comment lines.
    private static int cmtLines = 0;

    // Total num of single line comments.
    private static int singleLines = 0;

    // Total num of block comments.
    private static int block = 0;

    // Total num of lines within block comments.
    private static int blockLines = 0;

    // Total num of TODO.
    private static int todo = 0;

    private static ArrayList<File> fileList;

    static {
        fileList = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {

        // Get the folder path from console.
        // String folderpath = "testcode".
        System.out.println("Enter the folder/file path: ");

        Scanner scanner = new Scanner(System.in);
        String path = scanner.nextLine();

        File file = new File(path);
        // Input path is a valid folder.
        if(file.isDirectory()) scanFolder(file);
        else{
            // Update: Input path is a file.
            String fileName = file.getName();
            // Ignore the filename starts with a "." and without an extension.
            if(!(fileName.charAt(0) == '.' || !fileName.contains("."))){
                fileList.add(file);
            }else{
                System.out.println("No valid input file!");
                return;
            }
        }


        for(File f : fileList){

            // Determine the file type.
            String filename = f.getName();
            String fileType = filename.substring(filename.indexOf('.')+1);

            // Count the total number of lines.
            countLines(f);

            // Count the number of comment lines.
            countComments(f, fileType);

            // Print out the outcome.
            System.out.println("\nThe summary of "+filename+" is:");
            System.out.println("Total # of lines: "+ lines);
            System.out.println("Total # of cmtLines: "+ cmtLines);
            System.out.println("Total # of singleLines: "+ singleLines);
            System.out.println("Total # of block: "+ block);
            System.out.println("Total # of blockLines: "+ blockLines);
            System.out.println("Total # of todo: "+ todo);

            reset();
        }

        scanner.close();
    }

    /**
     * This method Recursively add all files within the folder into fileList.
     * @param file an folder for scanning
    */
    private static void scanFolder(File file){
        File[] files = file.listFiles();
        assert files != null;
        for(File f: files) {
            if (f.isDirectory()) {
                scanFolder(f);
            } else {
                String fileName = f.getName();
                // Ignore the filename starts with a "." and without an extension.
                if(!(fileName.charAt(0) == '.' || !fileName.contains("."))){
                    fileList.add(f);
                }
            }
        }
    }


    /**
     * This method count the total number of lines within the file.
     * @param f an file for scanning
     */
    private static void countLines(File f) throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        while((br.readLine()) != null){
            lines++;
        }
        br.close();
    }

    /**
     * This method count count the comment lines according to different file types
     * Two basic case for java(javascript) and python
     * One case for self-defined patterns
     * @param f an file for scanning
     * @param fileType the type of the specific file
     */
    private static void countComments(File f, String fileType) throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String line;

        switch (fileType) {

            //single comment: single line start with double slash ("//"),
            //block comment: continuous multiple lines start with "/*" end with "*\/"
            case "java":
            case "js":

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    countSingle(line, "//");
                    br = countBlock(line, br, "/*", "*/");
                }

                // Add up single comment lines and block comment lines to total comment lines.
                cmtLines = singleLines + blockLines;
                break;


            //single comment: single line start with "#",
            //block comment: continuous multiple lines start with Hashtag(#) or
            case "py":

                // InBlock means the previous line is in a comment block
                boolean inBlock = false;
                // FirstCmtLine means the previous line is a comment line not included in any block comment
                boolean firstCmtLine = false;

                while ((line = br.readLine()) != null) {
//                    br = countBlock(line, br, "\"\"\"", "\"\"\"");
//                    br = countBlock(line, br, "\'\'\'", "\'\'\'");

                    if (line.startsWith("#")) {
                        // The current line is a comment line start with "#".
                        todo += countTODO(line);
                        if (inBlock) {
                            // The previous line is already in a comment block, so just add up blockLines.
                            blockLines++;
                        } else if (firstCmtLine) {
                            // The previous line is a comment line but not included in a block, so come up with a new block by combining current line with the previous one.
                            block++;
                            blockLines += 2;
                            inBlock = true;
                            firstCmtLine = false;
                        } else {
                            // The previous line isn't a comment line.
                            firstCmtLine = true;
                        }
                    } else if (line.contains("#")) {
                        // Count the single comment at the end of line.
                        singleLines++;
                        int index = line.indexOf("#");
                        String subline = line.substring(index);
                        todo += countTODO(subline);
                    } else {
                        // Current line isn't a comment line while the previous line is, the previous line is a single comment line.
                        if (firstCmtLine) {
                            singleLines++;
                            firstCmtLine = false;
                        }
                        inBlock = false;
                    }
                }
                // Add up single comment lines and block comment lines to total comment lines.
                cmtLines = singleLines + blockLines;
                break;

            default:
                System.out.println("\nPlease specify the comment style of " + fileType + ":");
                System.out.println("Please enter the single comment pattern(Press Enter if not exists): ");
                Scanner s = new Scanner(System.in);
                String singlePattern = s.nextLine();
                System.out.println("Please enter the start of block comment pattern(Press Enter if not exists): ");
                String blockStartPattern = s.nextLine();
                System.out.println("Please enter the end of block comment pattern(Press Enter if not exists): ");
                String blockEndPattern = s.nextLine();

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (singlePattern.length() != 0) {
                        countSingle(line, singlePattern);
                    }
                    if (blockStartPattern.length() > 0 && blockEndPattern.length() > 0) {
                        br = countBlock(line, br, blockStartPattern, blockEndPattern);
                    }
                }
                // Add up single comment lines and block comment lines to total comment lines.
                cmtLines = singleLines + blockLines;
                break;
        }
        br.close();
    }

    /**
     *  This method count the total number of todo within the file.
     * @param line an comment String
     */
    private static int countTODO(String line){
        int cntTODO = 0;
        // Specify the pattern which could occur in the comment several times.
        Matcher m = Pattern.compile("(?=(TODO))").matcher(line);
        while(m.find()){
            cntTODO++;
        }
        return  cntTODO;

    }

    /**
     *  This method count the total number of single comment in the file.
     * Two types of single comments:
     *   1. the line start with specific pattern,
     *   2. the coding line is combined with a comment line at the end.
     * @param line a line within the file
     * @param start the single comment pattern
     */
    private  static void countSingle(String line, String start) {
        if(line.startsWith(start)){
            // Count the line start with specific pattern.
            singleLines++;
            todo  += countTODO(line);

        }else if(line.contains(start)) {
            // Count the single comment at the end of line.
            singleLines++;
            int index = line.indexOf(start);
            String subline = line.substring(index);
            todo += countTODO(subline);
        }

    }

    /**
     *  This method count the # of block comments and lines within block comments
     * @param line a line within the file
     * @param br BufferReader
     * @param start a start pattern of block
     * @param end an end pattern of block
     * @return BufferReader variable
     */
    private static BufferedReader countBlock(String line, BufferedReader br, String start, String end)throws IOException{
        if(line.startsWith(start)){
            block++;
            blockLines++;
            todo  += countTODO(line);

            while(!line.endsWith(end) && ((line = br.readLine()) != null)){
                line = line.trim();
                todo  += countTODO(line);
                blockLines++;
            }
        }
        return br;
    }

    /**
     *  This method resets all variables
     */
    private static void reset(){
        lines = 0;
        cmtLines = 0;
        singleLines = 0;
        block = 0;
        blockLines = 0;
        todo = 0;
        fileList = new ArrayList<>();
    }
}