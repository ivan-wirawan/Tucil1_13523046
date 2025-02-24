import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Solver 
{
    static int N, M, P;
    static char[][] board;
    static List<char[][]> pieces = new ArrayList<>();
    static long attempts = 0;
    static long startTime;
    static Map<Character, Color> colorMap = new HashMap<>();
    static final String ANSI_RESET = "\u001B[0m";
    
    public static void main(String[] args) 
    {
        File selectedFile = selectFile();
        if (selectedFile == null) 
        {
            System.out.println("Tidak ada file yang dipilih.");
            return;
        }

        try 
        {
            Scanner scanner = new Scanner(selectedFile);
            if(!scanner.hasNextLine()) 
            {
                System.out.println("File kosong.");
                return;
            }
            String[] firstLine = scanner.nextLine().split(" ");
            if(firstLine.length != 3 || !String.join(" ", firstLine).matches("\\s*\\d+\\s+\\d+\\s+\\d+\\s*")) 
            {
                System.out.println("Format file tidak valid.");
                return;
            }
            try 
            {
            N = Integer.parseInt(firstLine[0]);
            M = Integer.parseInt(firstLine[1]);
            P = Integer.parseInt(firstLine[2]);
            } 
            catch (NumberFormatException e) 
            {
                System.out.println("Format Dimensi tidak valid.");
                return;
            }
            if (N <= 0 || M <= 0 || P <= 0) 
            {
                System.out.println("Dimensi harus lebih besar dari 0.");
                return;
            }

            String type = scanner.nextLine().trim();
            if(N*M < P) 
            {
                System.out.println("Jumlah kotak tidak mencukupi.");
                return;
            }
            else if (!type.equals("DEFAULT")) 
            {
                System.out.println("Tipe soal tidak valid.");
                return;
            }
            else 
            {
                List<String> allLines = new ArrayList<>();

                while (scanner.hasNextLine()) 
                {
                    allLines.add(scanner.nextLine());
                }
                scanner.close();

                int i = 0;
                while (i < allLines.size()) 
                {
                    ArrayList<String> currentPiece = new ArrayList<>();
                    char currentChar = allLines.get(i).charAt(0);
                    
                    
                    if (!colorMap.containsKey(currentChar)) 
                    {
                        colorMap.put(currentChar, pickColor());
                    }
                    
                    while (i < allLines.size() && allLines.get(i).charAt(0) == currentChar) 
                    {
                        currentPiece.add(allLines.get(i));
                        i++;
                    }
                    
                    pieces.add(convertMatrix(currentPiece));
                }
                board = new char[M][N];
                for (char[] row : board) Arrays.fill(row, '.');
                startTime = System.currentTimeMillis();
                if (solve(0)) 
                {
                    printSolution();
                    long endTime = System.currentTimeMillis();
                    System.out.println("Waktu proses: " + (endTime - startTime) + " ms");
                    System.out.println("Jumlah percobaan: " + attempts);
                    chooseFormatGUI();
                } else 
                {
                    System.out.println("Tidak ditemukan solusi.");
                }
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public static File selectFile() 
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File Puzzle Input");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        
        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) 
        {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    static Color pickColor() 
    {
        float hue = (float) Math.random();
        return Color.getHSBColor(hue, 1f, 1f);
    }

    static String getANSI(char c) 
    {
        if (c == '.') return "\u001B[37m";
        Color color = colorMap.get(c);
        if (color == null) return ANSI_RESET;
        
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    static char[][] convertMatrix(List<String> inputLines) 
    {
        int rows = inputLines.size();
        int cols = inputLines.stream().mapToInt(String::length).max().orElse(0);
        char[][] matrix = new char[rows][cols];
        for (int i = 0; i < rows; i++) 
        {
            String line = inputLines.get(i);
            for (int j = 0; j < cols; j++) 
            {
                matrix[i][j] = j < line.length() ? line.charAt(j) : ' ';
            }
        }
        return matrix;
    }
    
    static void chooseFormatGUI() 
    {
        String[] options = {"File Teks (TXT)", "File Gambar (JPG)", "Keduanya", "Tidak Disimpan"};
        int choice = JOptionPane.showOptionDialog
        (
            null,
            "Apakah Anda ingin mengexport dalam bentuk  file txt atau gambar?",
            "Export Format",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 3 || choice == JOptionPane.CLOSED_OPTION) 
        {
            return;
        }

        try 
        {
            if (choice == 0 || choice == 2) 
            {
                exportGUI("txt");
            }
            if (choice == 1 || choice == 2) 
            {
                exportGUI("jpg");
            }
        } 
        catch (IOException e) 
        {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void exportGUI(String ext) throws IOException 
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Solution as " + ext.toUpperCase());
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            ext.equals("txt") ? "Text File (*.txt)" : "JPEG Image (*.jpg)",
            ext
        ));
        
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) 
        {
            String path = fileChooser.getSelectedFile().getPath();
            if (!path.toLowerCase().endsWith("." + ext)) 
            {
                path += "." + ext;
            }
            
            if (ext.equals("jpg")) 
            {
                exportImage(path);
            } else 
            {
                exportText(path);
            }
            System.out.println("Solution exported to: " + path);
        }
    }

    static void exportImage(String path) throws IOException 
    {
        int cell = 30; 
        int font = 20; 
        BufferedImage image = new BufferedImage(N * cell, M * cell, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, N * cell, M * cell);
        
        g2d.setFont(new Font("Arial", Font.BOLD, font));
        FontMetrics metrics = g2d.getFontMetrics();
        
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) 
            {
                char c = board[i][j];
                if (c != '.') {
                    g2d.setColor(colorMap.get(c));
                    g2d.fillRect(j * cell, i * cell, cell, cell);
                    
                    String charStr = String.valueOf(c);
                    int textWidth = metrics.stringWidth(charStr);
                    int textHeight = metrics.getHeight();
                    int x = j * cell + (cell - textWidth) / 2;
                    int y = i * cell + (cell + textHeight) / 2 - metrics.getDescent();
                    
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(charStr, x, y);
                }
                g2d.setColor(Color.BLACK);
                g2d.drawRect(j * cell, i * cell, cell, cell);
            }
        }
        
        g2d.dispose();
        ImageIO.write(image, "jpg", new File(path));
    }

    

    static void exportText(String path) throws IOException 
    {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) 
        {
            for (char[] row : board) 
            {
                writer.println(new String(row));
            }
            
            writer.println("\nInformasi Proses Penyelesaian:");
            writer.printf("Waktu proses: %d ms%n", System.currentTimeMillis() - startTime);
            writer.printf("Jumlah percobaan: %d%n", attempts);
        }
    }

    static boolean solve(int idx) 
    {
        if (idx >= pieces.size()) return checkBoard();
        
        char[][] blok = pieces.get(idx);
        List<char[][]> transformations = generateVariants(blok);
        for (char[][] variant : transformations) 
        {
            for (int i = 0; i <= M - variant.length; i++) 
            {
                for (int j = 0; j <= N - variant[0].length; j++) 
                {
                   
                    if (canPlace(variant, i, j)) 
                    {
                        attempts++;
                        placeBlok(variant, i, j, true);
                        if (solve(idx + 1)) return true;
                        placeBlok(variant, i, j, false);
                    }
                }
            }
        }
        return false;
    }

    static boolean checkBoard() 
    {
        for (char[] row : board) 
        {
            for (char cell : row) 
            {
                if (cell == '.') return false;
            }
        }
        return true;
    }

    static boolean canPlace(char[][] blok, int r, int c) 
    {
        for (int i = 0; i < blok.length; i++) 
        {
            for (int j = 0; j < blok[i].length; j++) 
            {
                if (blok[i][j] != ' ' && board[r + i][c + j] != '.') {
                    return false;
                }
            }
        }
        return true;
    }

    static void placeBlok(char[][] blok, int r, int c, boolean place) 
    {
        for (int i = 0; i < blok.length; i++) 
        {
            for (int j = 0; j < blok[i].length; j++) 
            {
                if (blok[i][j] != ' ') 
                {
                    board[r + i][c + j] = place ? blok[i][j] : '.';
                }
            }
        }
    }

    

    static List<char[][]> generateVariants(char[][] blok) 
    {

        if (blok.length == 1 && blok[0].length == 1) 
        {
            return Collections.singletonList(blok);
        }

        Set<String> seen = new HashSet<>();
        List<char[][]> transformations = new ArrayList<>();
        char[][] current = blok;

        for (int i = 0; i < 4; i++) 
        {
            if (seen.add(Arrays.deepToString(current))) transformations.add(current);
            current = rotate(current);
        }
        
        current = flip(blok);
        for (int i = 0; i < 4; i++) 
        {
            if (seen.add(Arrays.deepToString(current))) transformations.add(current);
            current = rotate(current);
        }
        return transformations;
    }


    static char[][] rotate(char[][] matrix) 
    {
        int rows = matrix.length, cols = matrix[0].length;
        char[][] rotated = new char[cols][rows];
        for (int i = 0; i < rows; i++) 
        {
            for (int j = 0; j < cols; j++) 
            {
                rotated[j][rows - 1 - i] = matrix[i][j];
            }
        }
        return rotated;

    }

    static char[][] flip(char[][] matrix) 
    {
        int rows = matrix.length;
        char[][] flipped = new char[rows][];
        for (int i = 0; i < rows; i++) 
        {
            flipped[i] = Arrays.copyOf(matrix[i], matrix[i].length);
            for (int j = 0, k = matrix[i].length - 1; j < k; j++, k--) 
            {
                char temp = flipped[i][j];
                flipped[i][j] = flipped[i][k];
                flipped[i][k] = temp;

            }
        }
        return flipped;
    }

    static void printSolution() 
    {
        for (char[] row : board) 
        {
            for (char cell : row) 
            {
                System.out.print(getANSI(cell) + cell + ANSI_RESET);
            }

            System.out.println();
        }
    }
}