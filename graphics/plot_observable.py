import argparse
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description="Graficar evolucion temporal del observable va")
    parser.add_argument("--timestamp", required=True, help="Timestamp de la corrida")
    parser.add_argument("--data-dir", default="data", help="Directorio de datos")
    args = parser.parse_args()

    file_path = Path(args.data_dir) / f"{args.timestamp}-order.txt"

    if not file_path.exists():
        print(f"Error: No se encontro el archivo {file_path}")
        return

    # Leer iteraciones y valores de orden
    data = np.loadtxt(file_path)
    iterations = data[:, 0]
    va = data[:, 1]

    # Graficar
    plt.figure(figsize=(10, 5))
    plt.plot(iterations, va, label='Polarizacion $v_a$', color='blue', linewidth=1.5)

    # Configuracion del grafico
    plt.title("Evolucion temporal del observable ($v_a$)")
    plt.xlabel("Iteracion (t)")
    plt.ylabel("Polarizacion ($v_a$)")
    plt.ylim(0, 1.05)
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.legend()

    plt.tight_layout()
    plt.show()

if __name__ == "__main__":
    main()