import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np


def main():
    parser = argparse.ArgumentParser(description="Graficar orden vs eta para corrida C")
    parser.add_argument("--timestamp", required=True, help="Timestamp de la corrida (sin sufijos)")

    parser.add_argument("--data-dir", default="data", help="Directorio de datos")
    args = parser.parse_args()

    file_path = None
    scenario = None
    for i in range(3): # Check for scenarios 0, 1, 2
        path = Path(args.data_dir) / f"{args.timestamp}C-scenario{i}.txt"
        if path.exists():
            file_path = path
            scenario = i
            break

    if file_path is None:
        print(f"Error: No se encontro el archivo para el timestamp {args.timestamp}C-scenario[0-2].txt")
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
        plt.errorbar(eta, order, yerr=error, fmt="o-", linewidth=1.5, color="tab:blue", ecolor="tab:orange", elinewidth=1, capsize=3)
        y_low = np.min(order - error)
        y_high = np.max(order + error)
    else:
        plt.plot(eta, order, marker="o", linestyle="-", linewidth=1.5, color="tab:blue")
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
    plt.xlabel(r"$\eta$")
    plt.ylabel(r"$\langle v_a \rangle$")
    plt.grid(True, linestyle="--", alpha=0.7)

    output_dir = Path(args.data_dir) / "plots"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"order_vs_eta_C_sc{scenario}_{args.timestamp}.png"

    plt.tight_layout()
    plt.savefig(output_path, dpi=300)
    print(f"Grafico guardado en {output_path}")


if __name__ == "__main__":
    main()

