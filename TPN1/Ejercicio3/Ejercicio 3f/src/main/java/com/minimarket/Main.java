package com.minimarket;

import com.minimarket.entities.Cliente;
import com.minimarket.entities.Factura;
import com.minimarket.enums.EstadoFactura;
import com.minimarket.services.ClienteService;
import com.minimarket.services.FacturaService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final ClienteService clienteService = new ClienteService();
    private static final FacturaService facturaService = new FacturaService();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    public static void main(String[] args) {
        int opcion;
        do {
            System.out.println("\n========================================");
            System.out.println("     MINIMARKET - SISTEMA DE GESTION    ");
            System.out.println("========================================");
            System.out.println("1. Gestionar Clientes");
            System.out.println("2. Gestionar Facturas");
            System.out.println("0. Salir");
            System.out.println("========================================");
            System.out.print("Seleccione una opcion: ");
            opcion = leerEntero();

            switch (opcion) {
                case 1 -> menuClientes();
                case 2 -> menuFacturas();
                case 0 -> System.out.println("Saliendo del sistema...");
                default -> System.out.println("Opcion invalida.");
            }
        } while (opcion != 0);
    }

    private static void menuClientes() {
        int opcion;
        do {
            System.out.println("\n--- GESTION DE CLIENTES ---");
            System.out.println("1. Alta de Cliente");
            System.out.println("2. Modificar Cliente");
            System.out.println("3. Baja de Cliente (logica)");
            System.out.println("4. Buscar Cliente por ID");
            System.out.println("5. Listar todos los Clientes");
            System.out.println("6. Listar Clientes activos");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            opcion = leerEntero();

            switch (opcion) {
                case 1 -> altaCliente();
                case 2 -> modificarCliente();
                case 3 -> bajaCliente();
                case 4 -> buscarCliente();
                case 5 -> listarClientes();
                case 6 -> listarClientesActivos();
                case 0 -> {}
                default -> System.out.println("Opcion invalida.");
            }
        } while (opcion != 0);
    }

    private static void menuFacturas() {
        int opcion;
        do {
            System.out.println("\n--- GESTION DE FACTURAS ---");
            System.out.println("1. Alta de Factura");
            System.out.println("2. Modificar Factura");
            System.out.println("3. Baja de Factura (logica)");
            System.out.println("4. Buscar Factura por ID");
            System.out.println("5. Listar todas las Facturas");
            System.out.println("6. Listar Facturas activas");
            System.out.println("7. Listar Facturas por Estado");
            System.out.println("0. Volver");
            System.out.print("Seleccione una opcion: ");
            opcion = leerEntero();

            switch (opcion) {
                case 1 -> altaFactura();
                case 2 -> modificarFactura();
                case 3 -> bajaFactura();
                case 4 -> buscarFactura();
                case 5 -> listarFacturas();
                case 6 -> listarFacturasActivas();
                case 7 -> listarFacturasPorEstado();
                case 0 -> {}
                default -> System.out.println("Opcion invalida.");
            }
        } while (opcion != 0);
    }

    private static void altaCliente() {
        System.out.println("\n>> ALTA DE CLIENTE");
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Apellido: ");
        String apellido = scanner.nextLine();
        System.out.print("DNI: ");
        String dni = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();

        try {
            clienteService.crearCliente(nombre, apellido, dni, email);
            System.out.println("Cliente creado exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al crear cliente: " + e.getMessage());
        }
    }

    private static void modificarCliente() {
        System.out.println("\n>> MODIFICAR CLIENTE");
        listarClientesActivos();
        System.out.print("Ingrese el ID del cliente a modificar: ");
        String id = scanner.nextLine();

        Cliente cliente = clienteService.buscarCliente(id);
        if (cliente == null) {
            System.out.println("Cliente no encontrado.");
            return;
        }

        System.out.println("Datos actuales: " + cliente);
        System.out.print("Nuevo nombre (" + cliente.getNombre() + "): ");
        String nombre = scanner.nextLine();
        if (nombre.isEmpty()) nombre = cliente.getNombre();

        System.out.print("Nuevo apellido (" + cliente.getApellido() + "): ");
        String apellido = scanner.nextLine();
        if (apellido.isEmpty()) apellido = cliente.getApellido();

        System.out.print("Nuevo DNI (" + cliente.getDni() + "): ");
        String dni = scanner.nextLine();
        if (dni.isEmpty()) dni = cliente.getDni();

        System.out.print("Nuevo email (" + cliente.getEmail() + "): ");
        String email = scanner.nextLine();
        if (email.isEmpty()) email = cliente.getEmail();

        try {
            clienteService.modificarCliente(id, nombre, apellido, dni, email);
            System.out.println("Cliente modificado exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al modificar cliente: " + e.getMessage());
        }
    }

    private static void bajaCliente() {
        System.out.println("\n>> BAJA DE CLIENTE");
        listarClientesActivos();
        System.out.print("Ingrese el ID del cliente a eliminar: ");
        String id = scanner.nextLine();

        try {
            clienteService.eliminarCliente(id);
            System.out.println("Cliente eliminado exitosamente (baja logica).");
        } catch (Exception e) {
            System.out.println("Error al eliminar cliente: " + e.getMessage());
        }
    }

    private static void buscarCliente() {
        System.out.print("Ingrese el ID del cliente: ");
        String id = scanner.nextLine();
        Cliente cliente = clienteService.buscarCliente(id);
        if (cliente != null) {
            System.out.println(cliente);
        } else {
            System.out.println("Cliente no encontrado.");
        }
    }

    private static void listarClientes() {
        List<Cliente> clientes = clienteService.listarClientes();
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados.");
        } else {
            System.out.println("\n--- LISTADO DE CLIENTES ---");
            clientes.forEach(System.out::println);
        }
    }

    private static void listarClientesActivos() {
        List<Cliente> clientes = clienteService.listarClientesActivos();
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes activos.");
        } else {
            System.out.println("\n--- CLIENTES ACTIVOS ---");
            clientes.forEach(System.out::println);
        }
    }

    private static void altaFactura() {
        System.out.println("\n>> ALTA DE FACTURA");

        List<Cliente> clientesActivos = clienteService.listarClientesActivos();
        if (clientesActivos.isEmpty()) {
            System.out.println("No hay clientes activos. Debe crear un cliente primero.");
            return;
        }
        System.out.println("Clientes disponibles:");
        clientesActivos.forEach(System.out::println);

        System.out.print("Numero de Factura: ");
        long nroFactura = leerLong();
        System.out.print("Fecha (dd/MM/yyyy): ");
        Date fecha = leerFecha();
        System.out.print("Total Pagado: ");
        double total = leerDouble();
        System.out.println("Estado (1=PAGADA, 2=ANULADA, 3=SIN_DEFINIR): ");
        EstadoFactura estado = leerEstado();
        System.out.print("ID del Cliente: ");
        String clienteId = scanner.nextLine();

        try {
            facturaService.crearFactura(nroFactura, fecha, total, estado, clienteId);
            System.out.println("Factura creada exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al crear factura: " + e.getMessage());
        }
    }

    private static void modificarFactura() {
        System.out.println("\n>> MODIFICAR FACTURA");
        listarFacturasActivas();
        System.out.print("Ingrese el ID de la factura a modificar: ");
        String id = scanner.nextLine();

        Factura factura = facturaService.buscarFactura(id);
        if (factura == null) {
            System.out.println("Factura no encontrada.");
            return;
        }

        System.out.println("Datos actuales: " + factura);

        System.out.print("Nuevo numero de factura (" + factura.getNumeroFactura() + "): ");
        String nroStr = scanner.nextLine();
        long nroFactura = nroStr.isEmpty() ? factura.getNumeroFactura() : Long.parseLong(nroStr);

        System.out.print("Nueva fecha (" + sdf.format(factura.getFechaFactura()) + "): ");
        String fechaStr = scanner.nextLine();
        Date fecha;
        if (fechaStr.isEmpty()) {
            fecha = factura.getFechaFactura();
        } else {
            try {
                fecha = sdf.parse(fechaStr);
            } catch (Exception e) {
                System.out.println("Fecha invalida, se mantiene la actual.");
                fecha = factura.getFechaFactura();
            }
        }

        System.out.print("Nuevo total (" + factura.getTotalPagado() + "): ");
        String totalStr = scanner.nextLine();
        double total = totalStr.isEmpty() ? factura.getTotalPagado() : Double.parseDouble(totalStr);

        System.out.print("Nuevo estado (" + factura.getEstado() + ") (1=PAGADA, 2=ANULADA, 3=SIN_DEFINIR, Enter=mantener): ");
        String estadoStr = scanner.nextLine();
        EstadoFactura estado;
        if (estadoStr.isEmpty()) {
            estado = factura.getEstado();
        } else {
            estado = switch (estadoStr) {
                case "1" -> EstadoFactura.PAGADA;
                case "2" -> EstadoFactura.ANULADA;
                default -> EstadoFactura.SIN_DEFINIR;
            };
        }

        System.out.println("Clientes disponibles:");
        clienteService.listarClientesActivos().forEach(System.out::println);
        System.out.print("Nuevo ID de cliente (" + factura.getCliente().getId() + "): ");
        String clienteId = scanner.nextLine();
        if (clienteId.isEmpty()) clienteId = factura.getCliente().getId();

        try {
            facturaService.modificarFactura(id, nroFactura, fecha, total, estado, clienteId);
            System.out.println("Factura modificada exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al modificar factura: " + e.getMessage());
        }
    }

    private static void bajaFactura() {
        System.out.println("\n>> BAJA DE FACTURA");
        listarFacturasActivas();
        System.out.print("Ingrese el ID de la factura a eliminar: ");
        String id = scanner.nextLine();

        try {
            facturaService.eliminarFactura(id);
            System.out.println("Factura eliminada exitosamente (baja logica).");
        } catch (Exception e) {
            System.out.println("Error al eliminar factura: " + e.getMessage());
        }
    }

    private static void buscarFactura() {
        System.out.print("Ingrese el ID de la factura: ");
        String id = scanner.nextLine();
        Factura factura = facturaService.buscarFactura(id);
        if (factura != null) {
            System.out.println(factura);
        } else {
            System.out.println("Factura no encontrada.");
        }
    }

    private static void listarFacturas() {
        List<Factura> facturas = facturaService.listarFacturas();
        if (facturas.isEmpty()) {
            System.out.println("No hay facturas registradas.");
        } else {
            System.out.println("\n--- LISTADO DE FACTURAS ---");
            facturas.forEach(System.out::println);
        }
    }

    private static void listarFacturasActivas() {
        List<Factura> facturas = facturaService.listarFacturasActivas();
        if (facturas.isEmpty()) {
            System.out.println("No hay facturas activas.");
        } else {
            System.out.println("\n--- FACTURAS ACTIVAS ---");
            facturas.forEach(System.out::println);
        }
    }

    private static void listarFacturasPorEstado() {
        System.out.print("Estado (1=PAGADA, 2=ANULADA, 3=SIN_DEFINIR): ");
        EstadoFactura estado = leerEstado();
        List<Factura> facturas = facturaService.listarFacturasPorEstado(estado);
        if (facturas.isEmpty()) {
            System.out.println("No hay facturas con estado " + estado + ".");
        } else {
            System.out.println("\n--- FACTURAS CON ESTADO " + estado + " ---");
            facturas.forEach(System.out::println);
        }
    }

    private static int leerEntero() {
        try {
            int valor = Integer.parseInt(scanner.nextLine().trim());
            return valor;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static long leerLong() {
        try {
            return Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double leerDouble() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Date leerFecha() {
        try {
            return sdf.parse(scanner.nextLine().trim());
        } catch (Exception e) {
            return new Date();
        }
    }

    private static EstadoFactura leerEstado() {
        int opcion = leerEntero();
        return switch (opcion) {
            case 1 -> EstadoFactura.PAGADA;
            case 2 -> EstadoFactura.ANULADA;
            default -> EstadoFactura.SIN_DEFINIR;
        };
    }
}
