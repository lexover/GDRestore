import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Quickstart {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "Drive API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_METADATA);

    public static final String ROOT_PATH = "/media/lexover/Media/GoogleDriveRestore/";

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                Quickstart.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    /**
     * Возвращает список файлов с установленными полями в соответствии с fields который
     * соответсвует переданному фильтру filter. Формат фильтра согласно документации GoogleDrive
     * как пример фильтр для выбора исключительно папок: "mimeType='application/vnd.google-apps.folder'"
     * fields также указывается в формате предусмотренном GoogleDrive
     * Как пример fields="files(id, name, parents)" считает поля id name и parens для фалов.
     * @param service - GoogleDrive сервис
     * @param filter строка фильтра
     * @param fields строка выбора полей
     * @return список фалов в соответствии с указанным фильтром, где установлены поля в соответствии с fields
     * @throws IOException
     */
    public static List<File> getAllFiles(Drive service, String filter, String fields) throws  IOException{
        List<File> result = new ArrayList<>();
        String pageToken = null;
        System.out.print("|");
        do {
            FileList fileList= service.files().list()
                    .setQ(filter)
                    .setSpaces("drive")
                    .setFields("nextPageToken, "+fields)
                    .setPageToken(pageToken)
                    .execute();
            pageToken = fileList.getNextPageToken();
            result.addAll(fileList.getFiles());
            System.out.print("=");
        }while (pageToken != null);
        System.out.println("|");
        return result;
    }

    /**
     * Выбирает все папки на диске. И возвращает их в виде списка файлов(GoogleDrive).
     * Каждая папка имеет свой id и имя. А также идентификаторы родителей (parents).
     * С помощью этих данных в дальнейшем можно строить дерево каталогов.
     * @param service - сервис GoogleDrive
     * @return список директорий с данными id, name, parents
     * @throws IOException
     */
    public static List<File> getDirectories(Drive service) throws IOException{
        List<File> result = getAllFiles(service,
                "mimeType='application/vnd.google-apps.folder'",
                "files(id, name, parents)");
        System.out.println("Loaded directory list ("+result.size()+" els).");
        return result;
    }

    /**
     * Выбирает все файлы на диске (которые не дирректории). И возвращает их в виде списка(GoogleDrive).
     * Для файлов получены данные - id, name, parents.
     * @param service - сервис GoogleDrive
     * @return список файлов с данными id, name, parents
     * @throws IOException
     */
    public static List<File> getFiles(Drive service) throws IOException{
        List<File> result = getAllFiles(service,
                "mimeType!='application/vnd.google-apps.folder'",
                "files(id, name, parents)");
        System.out.println("Loaded file list ("+result.size()+" els).");
        return result;
    }

    /**
     * Возвращает список ревизий для файла с указанным идентификатором (fileId), с установленными данными
     * о времени создания ревизии(modifiedTime), ее идентификатором и оригинальным именем файла.
     * @param service сервис GoogleDrive
     * @param fileId идентификатор файла для которого будут получены доступные ревизии
     * @return список ревизий доступных для файла с идентификатором fileId
     * @throws IOException
     */
    public static List<Revision> getRevisions(Drive service, String fileId) throws IOException{
        return service.revisions().list(fileId)
                .setFields("nextPageToken, revisions(id, modifiedTime, originalFilename)")
                .execute().getRevisions();
    }

    /**
     * Из переданных ревизий revs возвращает самую последнюю.
     * @param revs перечень ревизий из которых необходимо выбрать последнюю по дате
     * @return последнюю по дате ревизия
     * @throws IOException
     */
    public static Revision getLastRevision(List<Revision> revs) throws IOException{
        Revision result = revs.get(0);
        for(Revision r : revs) {
           if (r.getModifiedTime().getValue() > result.getModifiedTime().getValue()) result = r;
        }
        return result;
    }

    /**
     * Возвращает File с установленным идентификатором корневой папки GoogleDrive.
     * @param service GoogleDrive сервис
     * @return File с установленным идентификатором корневой папки GoogleDrive
     * @throws IOException
     */
    public static File getRoot(Drive service) throws IOException{
        return service.files().get("root").setFields("id").execute();
    }

    /**
     * В переданном списке директорий result выполнят записи в поле Directory.path на основе анализа parents.
     * Таким образом строится дерево каталогов (как таковы обычный список где в поле path указан полный путь к
     * каталогу).
     * Для работы используется полученный список директорий dirs. Все каталоги строятся относительно корневого
     * root. Таким образом если в root.getPath = "/MyRoot/" то path всех остальных директорий будет начианться
     * с  "/MyRoot/....".
     * Для построения используется рекурсия.
     * @param root корневая директория от которой будет строиться дерево
     * @param dirs список каталогов полученный с GoogleDrive для которого строится дерево
     * @param result список директорий в котором будет построено дерево.
     */
    public static void generateDirectoryTree(final Directory root, List<File> dirs, List<Directory> result){
       List<File> files = dirs.stream().filter((s) ->
               s.getParents().contains(root.getFile().getId())).collect(Collectors.<File>toList());
       for (File f : files) {
           Directory d = new Directory();
           d.setFile(f);
           d.setPath(root.getPath()+f.getName()+'/');
           result.add(d);
           dirs.remove(f);

           if (dirs.size() >= 0) generateDirectoryTree(d, dirs, result);
       }
    }

    /**
     * Создает каталоги в файловой системе в соответствии с переданным списком директорий dirs.
     * @param dirs список каталогов который должен быть создан на жестком диске
     */
    public static void createDirsInRoot(List<Directory> dirs){
        int i=0;
        for (Directory dir : dirs){
            java.io.File f = new java.io.File(dir.getPath());
            f.mkdirs();
            i++;
        }
        System.out.println("Created "+i+" dirs");
    }

    /**
     * Преобразует дату из строки в формате RFC3339 в LocalDateTime, отбрасывая данные о TimeZone
     * @param dateTimeStr строка в формате RFC3339
     * @return дата и время в формате LocalDateTime
     */
    public static LocalDateTime fromRFC3339(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr.substring(0, dateTimeStr.indexOf(".")), formatter);
    }

    /**
     * Загружает версию файла f которая была создана до указанного времени dateTime в каталог из списка
     * dirs который соответствует указанному в файле f.getParents().get(0).getId
     * @param service GoogleDrive сервис
     * @param dirs список доступных каталогов (для определения места расположения файла)
     * @param f файл версия которого должна быть загружена
     * @param dateTime дата до которой версия должна быть загружена
     * @throws IOException
     */
    public static void downloadFileRevisionByDate(Drive service, List<Directory> dirs, File f, LocalDateTime dateTime) throws IOException {

        List<Directory> directories = dirs.stream().filter((s) ->
                s.getFile().getId().equals(f.getParents().get(0))).collect(Collectors.<Directory>toList());
        String dirPath = (directories != null && ! directories.isEmpty()) ? directories.get(0).getPath() : ROOT_PATH;

        java.io.File filePath = new java.io.File(dirPath + f.getName());

        if (filePath.exists()) System.out.println("File exists: "+filePath.toString());
        else{

            Revision rev=null;
            LocalDateTime last = LocalDateTime.MIN;
            for (Revision r : getRevisions(service, f.getId())) {
                LocalDateTime revDate = fromRFC3339(r.getModifiedTime().toStringRfc3339());
                if (revDate.isBefore(dateTime) && revDate.isAfter(last)) {
                    last = revDate;
                    rev = r;
                }
            }

            OutputStream outputStream = new FileOutputStream(filePath);
            if(rev != null) {
                System.out.print("Rev."+rev.getModifiedTime().toString()+" "+filePath.toString());
                service.revisions().get(f.getId(), rev.getId()).executeMediaAndDownloadTo(outputStream);
                System.out.println(" End!");
            } else {
                System.out.println("\nError get revision for file: " + filePath.toString() + "\n");
            }
        }
    }


    /**
     * Загружает на диск в указанную корневую директорию (ROOT_PATH), весь диск с ревизиями файлов на дату
     * до указанной в toDate.
     * @param service
     * @param toDate дата до которой файлы должны быть восстановлены
     * @throws IOException
     */
    public static void recoverFilesToDate(Drive service, LocalDateTime toDate) throws IOException{

        //Корневая директория - там где будет воссоздана копия GoogleDrive
        Directory root = new Directory();
        root.setPath(Quickstart.ROOT_PATH);
        root.setFile(getRoot(service));

        //Получаем список директорий содержащихся на GoogleDrive
        List<Directory> dirs = new ArrayList<>();
        generateDirectoryTree(root, getDirectories(service), dirs);

        //Создаем дерево каталогов внутри корневой директории
        createDirsInRoot(dirs);

        //Загружаем все доступные файлы
        List<File> files = getFiles(service);
        int i=0;
        for (File file : files){
            i++;
            System.out.print(String.format("%04d ", i));
            try{
                //Загружаем последние версии файлов созданные до указанной даты.
                downloadFileRevisionByDate(service, dirs, file, toDate);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Удаляет на GoogleDrive все файлы с указанным именем name
     * @param service сервис диска
     * @param name имя файлов которые нужно удалить
     * @throws IOException
     */
    public static void deleteFilesWithName(Drive service, String name) throws IOException{
        int i=0;
        for (File f : getAllFiles(service, "name='" + name +"'", "files(id, name)")){
            service.files().delete(f.getId()).execute();
            i++;
            System.out.println("Deleted file " + f.getName());
        }
        System.out.println("Deleted "+i+" files");
    }


    /** Выполняет восстановление файлов которые были зашифрованы CTB-Locker (имеют расширения .CTB-Locker).
     *  Происходит следующим образом: выбираются все файлы находящиеся на GoogleDrive считываются все ревизии
     *  и в поле ревизии originFileName проверяется наличие того самого расширения CTB-Locker. Если оно
     *  присутствует и количество ревизий более чем 1 то данная ревизия удаляется. После чего доступ к файлу
     *  осущесвтляется по предыдущей ревизии. Если же кол-во ревизий менее 2-х то в файл DontRestoredFiles.txt
     *  находящийся в директории с текущей программой записывается сообщение, что данный файл не был восстановлен
     *  к предыдущей версии.
     *  Кроме того осуществляется переименование файла, т.е. удаляется это расширение CTB-Locker.
     * @param service
     * @throws IOException
     */
    public static void restoreFiles(Drive service) throws IOException{
        java.io.File logFile = new java.io.File("DontRestoredFiles.txt");
        try {
            if (!logFile.exists()){
                logFile.createNewFile();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        try(FileWriter writer = new FileWriter(logFile, true)) {
            writer.write("\n========================================\n");
            String locker = ".CTB-Locker";
            List<File> files = getFiles(service);
            for (File f : files) {
                if (f.getName().contains(locker)) {
                    List<Revision> revs = getRevisions(service, f.getId());
                    if (revs.size() > 1) {
                        for (Revision rev : revs) {
                            if (rev.getOriginalFilename().contains(locker)) {

                                try{

                                    System.out.printf("Remove rev(%s) for file: %s\n", rev.toString(), f.getName());
                                    service.revisions().delete(f.getId(), rev.getId()).execute();
                                } catch (IOException e){
                                   writer.write("Error delete revision "+rev+" for file: "+f.getName());
                                   e.printStackTrace();

                                }

                                String newName = f.getName().replace(locker, "");
                                try {
                                    File newFile = new File();
                                    newFile.setName(newName);
                                    service.files().update(f.getId(), newFile).execute();
                                    System.out.printf("Rename %s to %s\n", f.getName(), newName);
                                } catch (IOException e){
                                    writer.write("Error rename file from: "+f.getName()+" to: "+newName);
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        writer.write(f.getName() + "\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        // Build a new authorized API client service.
        Drive service = getDriveService();

        //Мое восстановление после загрузки шифрованных CTB-Locker файлов на диск следующее:
        // Изначально я загрузил фалы в версии которая была до даты шифрования файлов
        //Так в моем случае (посмотрел в веб GoogleDrive дату последнего изменения файлов - которая оказалась
        //12.05.2017. Таким образом файлы были загружены на мой жесткий диск в ревизии до момента их шифрования.

        //Дата до которой файлы не были испорчены
        //recoverFilesToDate(service, LocalDateTime.of(2017, 5, 12, 0, 0));

        //Далее можно было приступить к манипуляциями непосредственно с диском.
        //Для начала я решил удалить все файлы txt с сообщением шифровщика.

        //deleteFilesWithName(service, "!__П_Р_О_Ч_Т_И__CTB-Locker__ПРОЧТИ__ПРОЧТИ.TXT");

        //После этого приступил к восстановлению файлов на GoogleDrive к ревизии до шифрования
        restoreFiles(service);

        //Осталось  проверить файл "DontRestoredFiles.txt" в папке с текущей программой.
        //Если там есть ошибки придется проверить их вручную.
        //Этот файл дополняется при каждом запуске программы (старые сообщения не удаляются новые записываютс
        // в конец файла), cообщения предыдущего запуска отделяются от текущего "===================".
    }
}