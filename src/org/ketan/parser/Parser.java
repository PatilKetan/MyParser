package org.ketan.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
	public static void main(String[] args) throws IOException {
		Parser parser = new Parser();
		parser.readInputAndCreateResources(args[0]);
	}

	private void readInputAndCreateResources(String inputFilePath) throws IOException {

		int pos = inputFilePath.lastIndexOf("/");
		String userDirectory = inputFilePath.substring(0, pos);
		String userFileName = trimFileExtension(inputFilePath.substring(pos + 1));

		Stream<String> stream = Files.lines(Paths.get(inputFilePath));
		List<String> fileData = stream.collect(Collectors.toList());
		stream.close();

		createEntityFile(fileData, userDirectory, userFileName);
		createDTOFile(fileData, userDirectory, userFileName);
		createInDTOFile(fileData, userDirectory, userFileName);
		createLiquibaseChangeSetFile(fileData, userDirectory, userFileName);

	}

	private void createLiquibaseChangeSetFile(List<String> fileData, String directory, String originalFileName)
			throws IOException {
		String changeSetFileName = covertToCamelCase(originalFileName, true);
		File changeSetFile = new File(directory + "/" + changeSetFileName + ".xml");
		changeSetFile.createNewFile();
		writeDataToLiquibaseChangeSetFile(changeSetFile, originalFileName, changeSetFileName, fileData);
	}

	private void writeDataToLiquibaseChangeSetFile(File changeSetFile, String originalFileName, String entityFileName,
			List<String> fileData) throws IOException {
		Iterator<String> iterator = fileData.iterator();
		FileWriter fileWriter = new FileWriter(changeSetFile);
		String lineBreak = System.getProperty("line.separator");

		fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + lineBreak);
		fileWriter.write("<databaseChangeLog\n" + "	xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n"
				+ "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "	xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.0.xsd\">\n"
				+ "" + lineBreak);
		fileWriter.write("<changeSet author=\"***author***\" id=\"1\">" + lineBreak);
		fileWriter.write("<createTable tableName=\"" + originalFileName + "\" remarks=\"" + originalFileName + "\">"+ lineBreak);
		while (iterator.hasNext()) {
			String currLine = iterator.next().trim();
			String currInputs[] = currLine.split(",");

			String databaseColumnName = currInputs[0].toLowerCase();
			String dataType = getLiqubaseDataType(currInputs[1]);

			fileWriter.write("<column name=\"" + databaseColumnName + "\" type=\"" + dataType + "\" >" + lineBreak);
			fileWriter.write("</column>" + lineBreak);
		}
		fileWriter.write("</createTable>" + lineBreak);
		fileWriter.write("</changeSet>" + lineBreak);
		fileWriter.write("</databaseChangeLog>" + lineBreak);
		fileWriter.close();
	}

	private void createInDTOFile(List<String> fileData, String directory, String originalFileName) throws IOException {
		String inDTOFileName = covertToCamelCase(originalFileName, true) + "InDTO";
		File inDTOFile = new File(directory + "/" + inDTOFileName + ".java");
		inDTOFile.createNewFile();
		writeDataToDTOFile(inDTOFile, originalFileName, inDTOFileName, fileData);
	}

	private void createDTOFile(List<String> fileData, String directory, String originalFileName) throws IOException {
		String outDTOFileName = covertToCamelCase(originalFileName, true) + "DTO";
		File outDTOFile = new File(directory + "/" + outDTOFileName + ".java");
		outDTOFile.createNewFile();
		writeDataToDTOFile(outDTOFile, originalFileName, outDTOFileName, fileData);
	}

	private void writeDataToDTOFile(File DTOFile, String originalFileName, String DTOFileName, List<String> fileData)
			throws IOException {
		Iterator<String> iterator = fileData.iterator();
		FileWriter fileWriter = new FileWriter(DTOFile);
		String lineBreak = System.getProperty("line.separator");

		fileWriter.write("@Data" + lineBreak);
		fileWriter.write("@EqualsAndHashCode(callSuper = false)" + lineBreak);
		fileWriter.write("public class " + DTOFileName + lineBreak);
		fileWriter.write("{" + lineBreak);
		while (iterator.hasNext()) {
			String currLine = iterator.next().trim();
			String currInputs[] = currLine.split(",");

			String databaseColumnName = currInputs[0];
			String dataType = getJavaDataType(currInputs[1]);
			String propertyName = covertToCamelCase(databaseColumnName, false);

			fileWriter.write("private " + dataType + " " + propertyName + ";" + lineBreak);
		}
		fileWriter.write("}");
		fileWriter.close();
	}

	private void createEntityFile(List<String> fileData, String directory, String originalFileName) throws IOException {
		String entityFileName = covertToCamelCase(originalFileName, true) + "Entity";
		File entityFile = new File(directory + "/" + entityFileName + ".java");
		entityFile.createNewFile();
		writeDataToEntityFile(entityFile, originalFileName, entityFileName, fileData);
	}

	private void writeDataToEntityFile(File entityFile, String originalFileName, String entityFileName,
			List<String> fileData) throws IOException {
		Iterator<String> iterator = fileData.iterator();
		FileWriter fileWriter = new FileWriter(entityFile);
		String lineBreak = System.getProperty("line.separator");

		fileWriter.write("@Data" + lineBreak);
		fileWriter.write("@Entity" + lineBreak);
		fileWriter.write("@Table(name = \"" + originalFileName + "\")" + lineBreak);
		fileWriter.write("public class " + entityFileName + lineBreak);
		fileWriter.write("{");
		while (iterator.hasNext()) {
			String currLine = iterator.next().trim();
			String currInputs[] = currLine.split(",");

			String databaseColumnName = currInputs[0].toLowerCase();
			String dataType = getJavaDataType(currInputs[1]);
			String propertyName = covertToCamelCase(databaseColumnName, false);

			fileWriter.write(lineBreak);
			fileWriter.write("@Column(name = \"" + databaseColumnName + "\")" + lineBreak);
			fileWriter.write("private " + dataType + " " + propertyName + ";" + lineBreak);
		}
		fileWriter.write("}");
		fileWriter.close();
	}

	private String covertToCamelCase(String snakeCaseString, boolean isFileOrClassName) {
		StringBuilder sb = new StringBuilder(snakeCaseString.toLowerCase());
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '_') {
				sb.deleteCharAt(i);
				sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
			}
		}
		if (isFileOrClassName) {
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		}
		return sb.toString();
	}

	private String getJavaDataType(String dataTypeString) {
		Character dataTypeCode = dataTypeString.charAt(0);
		if (dataTypeCode == 'I' || dataTypeCode == 'i') {
			return "Integer";
		} else if (dataTypeCode == 'S' || dataTypeCode == 's') {
			return "String";
		} else if (dataTypeCode == 'b') {
			return "boolean";
		} else if (dataTypeCode == 'B') {
			return "BigDecimal";
		} else if (dataTypeCode == 'F' || dataTypeCode == 'f') {
			return "Float";
		} else if (dataTypeCode == 'D' || dataTypeCode == 'd') {
			return "LocalDate";
		} else if (dataTypeCode == 'T' || dataTypeCode == 't') {
			return "LocalDateTime";
		} else if (dataTypeCode == 'E' || dataTypeCode == 'e') {
			return "***ENTER_ENUM***";
		} else {
			return "***ENTER_DATATYPE***";
		}
	}
	private String getLiqubaseDataType(String dataTypeString) {
		Character dataTypeCode = dataTypeString.charAt(0);
		if (dataTypeCode == 'I' || dataTypeCode == 'i') {
			return "integer";
		} else if (dataTypeCode == 'S' || dataTypeCode == 's') {
			return "varchar(32)";
		} else if (dataTypeCode == 'b') {
			return "boolean";
		} else if (dataTypeCode == 'B') {
			return "decimal(17,2)";
		} else if (dataTypeCode == 'F' || dataTypeCode == 'f') {
			return "decimal(5,2)";
		} else if (dataTypeCode == 'D' || dataTypeCode == 'd') {
			return "date";
		} else if (dataTypeCode == 'T' || dataTypeCode == 't') {
			return "timestamp";
		} else if (dataTypeCode == 'E' || dataTypeCode == 'e') {
			return "***ENTER_ENUM***";
		} else {
			return "***ENTER_DATATYPE***";
		}
	}

	private String trimFileExtension(String userFileAndExtension) {
		int pos;
		pos = userFileAndExtension.indexOf(".");
		String userFileName;
		if (pos != -1) {
			userFileName = userFileAndExtension.substring(0, pos);
		} else {
			userFileName = userFileAndExtension;
		}
		return userFileName;
	}

}