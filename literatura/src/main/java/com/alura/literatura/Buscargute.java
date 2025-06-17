package com.alura.literatura;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.util.Scanner;

public class Buscargute {
    private static final String DB_URL = "jdbc:sqlite:busquedas.db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        crearTablaSiNoExiste();

        while (true) {
            System.out.println("\n--- MENÚ ---");
            System.out.println("1. Buscar libro");
            System.out.println("2. Ver historial de búsquedas");
            System.out.println("3. Buscar por título o autor");
            System.out.println("4. Salir");
            System.out.print("Seleccione una opción: ");
            int opcion = scanner.nextInt();
            scanner.nextLine(); // limpiar buffer

            switch (opcion) {
                case 1:
                    buscarLibro(scanner);
                    break;
                case 2:
                    mostrarHistorial();
                    break;
                case 3:
                    buscarEnHistorial(scanner);
                    break;
                case 4:
                    System.out.println("¡Hasta luego!");
                    return;
                default:
                    System.out.println("Opción inválida.");
            }
        }
    }

    private static void buscarLibro(Scanner scanner) {
        System.out.print("Ingrese el título del libro a buscar: ");
        String titulo = scanner.nextLine();

        try {
            String tituloCodificado = URLEncoder.encode(titulo, "UTF-8");
            String apiUrl = "https://gutendex.com/books?search=" + tituloCodificado;

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder resultado = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                resultado.append(linea);
            }
            br.close();

            JSONObject json = new JSONObject(resultado.toString());
            JSONArray libros = json.getJSONArray("results");

            if (libros.length() == 0) {
                System.out.println("No se encontraron libros con ese título.");
            } else {
                for (int i = 0; i < libros.length(); i++) {
                    JSONObject libro = libros.getJSONObject(i);
                    String tituloLibro = libro.getString("title");
                    int descargas = libro.getInt("download_count");

                    String autor = "Desconocido";
                    JSONArray autores = libro.getJSONArray("authors");
                    if (!autores.isEmpty()) {
                        autor = autores.getJSONObject(0).getString("name");
                    }

                    // Mostrar en consola
                    System.out.println("\nTítulo: " + tituloLibro);
                    System.out.println("Autor: " + autor);
                    System.out.println("Descargas: " + descargas);
                    System.out.println("------------------------------------");

                    // Guardar en la base de datos
                    guardarBusqueda(tituloLibro, autor, descargas);
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void mostrarHistorial() {
        String sql = "SELECT * FROM busquedas ORDER BY fecha_busqueda DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- HISTORIAL DE BÚSQUEDAS ---");

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Título: " + rs.getString("titulo"));
                System.out.println("Autor: " + rs.getString("autor"));
                System.out.println("Descargas: " + rs.getInt("descargas"));
                System.out.println("Fecha: " + rs.getString("fecha_busqueda"));
                System.out.println("----------------------------------");
            }

        } catch (SQLException e) {
            System.out.println("Error al consultar la base de datos: " + e.getMessage());
        }
    }

    private static void buscarEnHistorial(Scanner scanner) {
        System.out.print("Ingrese parte del título o nombre del autor: ");
        String criterio = scanner.nextLine();

        String sql = "SELECT * FROM busquedas WHERE titulo LIKE ? OR autor LIKE ? ORDER BY fecha_busqueda DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String patron = "%" + criterio + "%";
            pstmt.setString(1, patron);
            pstmt.setString(2, patron);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- RESULTADOS DE BÚSQUEDA ---");
            boolean hayResultados = false;
            while (rs.next()) {
                hayResultados = true;
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Título: " + rs.getString("titulo"));
                System.out.println("Autor: " + rs.getString("autor"));
                System.out.println("Descargas: " + rs.getInt("descargas"));
                System.out.println("Fecha: " + rs.getString("fecha_busqueda"));
                System.out.println("----------------------------------");
            }

            if (!hayResultados) {
                System.out.println("No se encontraron coincidencias.");
            }

        } catch (SQLException e) {
            System.out.println("Error al buscar en la base de datos: " + e.getMessage());
        }
    }

    private static void crearTablaSiNoExiste() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS busquedas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "titulo TEXT NOT NULL, " +
                    "autor TEXT, " +
                    "descargas INTEGER, " +
                    "fecha_busqueda TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(titulo, autor))";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla: " + e.getMessage());
        }
    }

    private static void guardarBusqueda(String titulo, String autor, int descargas) {
        String sql = "INSERT OR IGNORE INTO busquedas (titulo, autor, descargas) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, autor);
            pstmt.setInt(3, descargas);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al guardar en la base de datos: " + e.getMessage());
        }
    }
}
