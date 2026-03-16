import argparse
from pathlib import Path
import matplotlib.pyplot as plt
import numpy as np

def parse_b_file(file_path: Path):
    curves = []
    current_eta = None
    iterations = []
    orders = []

    with file_path.open() as fh:
        for raw_line in fh:
            line = raw_line.strip()
            if not line:
                continue
            if line.startswith("Eta:"):
                if current_eta is not None and iterations:
                    curves.append((current_eta, np.array(iterations, dtype=float), np.array(orders, dtype=float)))
                current_eta = float(line.split(":", 1)[1].strip())
                iterations = []
                orders = []
                continue
            parts = line.split()
            if len(parts) >= 2:
                iterations.append(float(parts[0]))
                orders.append(float(parts[1]))

    if current_eta is not None and iterations:
        curves.append((current_eta, np.array(iterations, dtype=float), np.array(orders, dtype=float)))

    return curves


def main():
    parser = argparse.ArgumentParser(description="Graficar orden vs iteracion para corrida B")
    parser.add_argument("--timestamp", required=True, help="Timestamp de la corrida (sin sufijos)")
    parser.add_argument("--data-dir", default="data", help="Directorio de datos")
    args = parser.parse_args()

    file_path = Path(args.data_dir) / f"{args.timestamp}B-B.txt"
    if not file_path.exists():
        print(f"Error: No se encontro el archivo {file_path}")
        return

    curves = parse_b_file(file_path)
    if not curves:
        print(f"Error: No se encontraron datos en {file_path}")
        return

    plt.figure(figsize=(10, 5))
    for eta, iterations, orders in curves:
        plt.plot(iterations, orders, label=f"eta = {eta}", linewidth=1.5)

    plt.title("Orden vs Iteracion (corrida B)")
    plt.xlabel("Iteracion (t)")
    plt.ylabel("Orden")
    plt.ylim(0, 1.05)
    plt.grid(True, linestyle="--", alpha=0.7)
    plt.legend()
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()

