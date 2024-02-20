package org.example;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Main {
    private static Set<List<String>> setUniqueStr = new HashSet<>(); // набор всех уникальных строчек (списки элементов)
    private static List<List<String>> haveDuplicatesList = new ArrayList<>(); //список всех строчек (списки элементов), имеющих дубликаты в столбцах с одинаковым номером
    private static Set<Set<List<String>>> allGroups = new HashSet<>(); //набор групп строчек

    public static void main(String[] args) throws IOException {

        //скачивание файла
        URL url = new URL("https://github.com/PeacockTeam/new-job/releases/download/v1.0/lng-4.txt.gz");
        InputStream inputStream = url.openStream();
        Files.copy(inputStream, new File("src/main/resources/lng-4.txt.gz").toPath());

        long begin = System.currentTimeMillis();

        //распаковка файла
        try {
            FileInputStream fis = new FileInputStream("src/main/resources/lng-4.txt.gz");
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream("src/main/resources/lng-4.txt");
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int lengthMax = 0; // максимальная длина строки

        //считываем строки из файла, помещаем в set
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/lng-4.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] arr = line.split(";");
                boolean rightStr = true;
                for (int i = 0; i < arr.length; i++) {
                    if (!arr[i].matches("\"\\d*\"")) {
                        rightStr = false;
                        break;
                    }
                }
                if (rightStr) {
                    List<String> arrayList = new ArrayList<>(Arrays.asList(arr));
                    if (lengthMax < arrayList.size()) lengthMax = arrayList.size();
                    setUniqueStr.add(arrayList);
                }
            }
        }


        //переводим набор срок в список
        List<List<String>> fullList = new ArrayList<>(setUniqueStr);

        setUniqueStr.clear();


        //создаем набор номеров строк из fullList, которые имеют совпадения значений столбцов
        Set<Integer> haveDuplicateNumberStr = new HashSet<>();

        //проходимся циклом по номерам столбцов
        //находим совпадения значений в столбцах, добавляем номера этих строк в haveDuplicateNumberStr
        for (int i = 0; i < lengthMax; i++) {
            Map<String, Integer> uniqueElements = new HashMap<>();
            for (int j = 0; j < fullList.size(); j++) {
                if (fullList.get(j).size() > i && !fullList.get(j).get(i).equals("\"\"")) {
                    if (!uniqueElements.containsKey(fullList.get(j).get(i))) {
                        uniqueElements.put(fullList.get(j).get(i), j);
                    } else {
                        haveDuplicateNumberStr.add(j);
                        haveDuplicateNumberStr.add(uniqueElements.get(fullList.get(j).get(i)));
                    }
                }
            }
        }


        //заполняем список строк для распределения по группам
        for (Integer number : haveDuplicateNumberStr) {
            haveDuplicatesList.add(fullList.get(number));
        }

        fullList.clear();

        //берем первую строку из haveDuplicatesList, добавляем ее в группу, из haveDuplicatesList удаляем
        // все группы добавляем в set
        int i = 0;
        while (true) {
            Set<List<String>> group = new HashSet<>();

            if (!haveDuplicatesList.isEmpty()) {
                allGroups.add(group);
                List<String> firstNode = haveDuplicatesList.get(0);
                group.add(firstNode);
                haveDuplicatesList.remove(0);
                groupUnion(firstNode, group);
            } else {
                break;
            }
            i++;
        }

        // трансформируем set всех групп в мапу, где ключ - размер группы, значение - List из всех групп, имеющих указанный размер
        Map<Integer, List<Set<List<String>>>> allGroupsMap = new TreeMap<>(Collections.reverseOrder());
        for (Set<List<String>> group : allGroups) {
            if (!allGroupsMap.containsKey(group.size())) {
                List<Set<List<String>>> singleGroupGroups = new ArrayList<>();
                singleGroupGroups.add(group);
                allGroupsMap.put(group.size(), singleGroupGroups);
            } else {
                allGroupsMap.get(group.size()).add(group);
            }
        }

        int allGroupsQuantity = allGroups.size();

        allGroups.clear();

        //записываем данные в файл
        String outputFileName = args[0];
        int numberGroup = 1;

        try (BufferedWriter writter = new BufferedWriter(new FileWriter(outputFileName))) {
            writter.write("Число групп с более чем одним элементом - " + allGroupsQuantity + "\n");
            for (Map.Entry<Integer, List<Set<List<String>>>> entry : allGroupsMap.entrySet()) {
                for (Set<List<String>> group : entry.getValue()) {
                    writter.write("Группа " + numberGroup + "\n");
                    for (List<String> str : group) {
                        writter.write(str + "\n");
                    }
                    numberGroup++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        long end = System.currentTimeMillis();

        System.out.println("Затрачено: " + (end - begin) + "мс");

    }

    //объединение в группы
    static void groupUnion(List<String> nodeList, Set<List<String>> group) {
        //переводим строку nodeList из списка элементов в мапу, где ключ - элемент, значение - список столбцов, в которых это знаяение находится в строке
        Map<String, List<Integer>> listElements = new HashMap<>();
        for (int i = 0; i < nodeList.size(); i++) {

            if (!nodeList.get(i).equals("\"\"")) {
                if (listElements.containsKey(nodeList.get(i))) {
                    listElements.get(nodeList.get(i)).add(i);
                } else {
                    List<Integer> numbersColumn = new ArrayList<>();
                    numbersColumn.add(i);
                    listElements.put(nodeList.get(i), numbersColumn);
                }
            }
        }

        // копируем все элементы из haveDuplicatesList в новый список
        List<List<String>> copyHaveDuplicatesList = new ArrayList<>(haveDuplicatesList);

        //обходим все элементы из копии haveDuplicatesList, сравниваем значения со зачениями nodeList
        //если находим совпадение в строке, запускаем метод groupUnion на этой строке
        for (int j = 0; j < copyHaveDuplicatesList.size(); j++) {
            for (int i = 0; i < copyHaveDuplicatesList.get(j).size(); i++) {
                if (listElements.get(copyHaveDuplicatesList.get(j).get(i)) != null) {
                    if (listElements.get(copyHaveDuplicatesList.get(j).get(i)).contains(i)) {
                        List<String> newNode = copyHaveDuplicatesList.get(j);
                        group.add(newNode);
                        haveDuplicatesList.remove(newNode);
                        if (haveDuplicatesList.isEmpty()) {
                            break;
                        }
                        groupUnion(newNode, group);
                    }
                }
            }
        }
    }
}

