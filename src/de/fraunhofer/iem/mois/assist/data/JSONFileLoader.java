package de.fraunhofer.iem.mois.assist.data;

import java.util.*;

public class JSONFileLoader {

    static private HashMap<String, Method> methods;
    static private String congFile = "";
    static public final int NEW_METHOD = 0;
    static public final int EXISTING_METHOD = 1;

    //Get configuration file location
    public static void setConfigurationFile(String path) {

        congFile = path;
    }

    //Get configuration file location
    public static String getConfigurationFile(boolean path) {

        if (path)
            return congFile;
        else
            return congFile.substring(congFile.lastIndexOf("/") + 1, congFile.length());

    }

    //Returns whether or not a configuration file was selected
    public static boolean isFileSelected() {

        return !getConfigurationFile(true).isEmpty();
    }

    //Import configuration details from JSON file
    public static void loadInitialFile() {

        JSONFileParser fileParser = new JSONFileParser(congFile);
        methods = fileParser.parseJSONFileMap();
    }

    //Compares new JSON file with original file and
    public static void loadUpdatedFile(String newFilePath) {

        JSONFileComparator fileComparator = new JSONFileComparator(congFile, newFilePath);
        methods = fileComparator.compareJSONFile();
        setConfigurationFile(newFilePath);
    }

    //Return list of methods as an array
    public static ArrayList<Method> getMethods() {

        return new ArrayList<>(methods.values());
    }


    //Return list of methods as an array using categories
    public static ArrayList<Method> getMethods(ArrayList<String> filters, String currentFile, boolean currentFileMode) {

        if (filters.size() == 0 && currentFileMode) {

            ArrayList<Method> filteredList = new ArrayList<>();

            for (String methodSignature : methods.keySet()) {

                if (methodSignature.contains(currentFile)) {
                    filteredList.add(methods.get(methodSignature));
                }
            }
            return filteredList;
        } else if (filters.size() > 0) {

            ArrayList<Method> filteredList = new ArrayList<>();

            for (String methodSignature : methods.keySet()) {

                if (methodSignature.contains(currentFile))
                    continue;

                for (Category category : methods.get(methodSignature).getCategories()) {

                    if (filters.contains(category.toString())) {
                        filteredList.add(methods.get(methodSignature));
                        break;
                    }
                }
            }
            return filteredList;
        } else
            return new ArrayList<>(methods.values());
    }

    //Return list of categories as a set
    public static Set<Category> getCategories() {

        Set<Category> categorySet = new HashSet<>();

        for (Method method : methods.values()) {

            for (Category category : method.getCategories()) {
                if (!categorySet.contains(category))
                    categorySet.add(category);
            }
        }
        return categorySet;
    }

    //Add new method to the list
    public static int addMethod(Method method) {


        if (methods.containsKey(method.getSignature(true))) {
            methods.replace(method.getSignature(true), method);
            return EXISTING_METHOD;
        } else {
            methods.put(method.getSignature(true), method);
            return NEW_METHOD;
        }
    }

    //Remove method from list
    public static void removeMethod(Method method) {

            methods.remove(method.getSignature(true));
    }
}
