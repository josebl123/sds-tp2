import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np


def main():
    parser = argparse.ArgumentParser(description="Graficar orden vs eta para corrida C")
    parser.add_argument("--timestamp", required=True, help="Timestamp de la corrida (sin sufijos)")
    parser.add_argument("--data-dir", default="data", help="Directorio de datos")
    args = parser.parse_args()

    file_path = Path(args.data_dir) / f"{args.timestamp}C-C.txt"
    if not file_path.exists():
        print(f"Error: No se encontro el archivo {file_path}")
        return

    try:
        data = np.loadtxt(file_path)
    except Exception as exc:  # noqa: BLE001
        print(f"Error al leer {file_path}: {exc}")
        return

    if data.ndim == 1 and data.size in (2, 3):
        data = data.reshape(1, -1)

    if data.shape[1] < 2:
        print(f"Error: Formato invalido en {file_path}")
        return

    eta = data[:, 0]
    order = data[:, 1]
    error = data[:, 2] if data.shape[1] >= 3 else None

    plt.figure(figsize=(8, 5))
    if error is not None:
        plt.errorbar(eta, order, yerr=error, fmt="o-", linewidth=1.5, color="tab:blue", ecolor="tab:orange", elinewidth=1, capsize=3, label="Orden ± error")
        y_low = np.min(order - error)
        y_high = np.max(order + error)
    else:
        plt.plot(eta, order, marker="o", linestyle="-", linewidth=1.5, color="tab:blue", label="Orden")
        y_low = np.min(order)
        y_high = np.max(order)

    padding = 0.05 * (y_high - y_low if y_high != y_low else 1)
    y_min = y_low - padding
    y_max = y_high + padding
    if y_min == y_max:  # avoid zero-height axis
        y_min -= 0.5
        y_max += 0.5

    plt.ylim(y_min, y_max)
    plt.margins(y=0.05)
    plt.title("Orden vs eta (corrida C)")
    plt.xlabel(r"$\eta$")
    plt.ylabel("Orden")
    plt.grid(True, linestyle="--", alpha=0.7)
    plt.legend()

    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()

