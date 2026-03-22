import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np


SCENARIO_LABELS = {
    0: "Sin líder (estándar)",
    1: "Líder dirección fija",
    2: "Líder circular",
}
SCENARIO_COLORS = {
    0: "tab:blue",
    1: "tab:orange",
    2: "tab:green",
}
SCENARIO_MARKERS = {
    0: "o",
    1: "s",
    2: "^",
}


def main():
    parser = argparse.ArgumentParser(
        description="Punto (d): Comparación de va vs eta para los 3 escenarios"
    )
    parser.add_argument(
        "--timestamp",
        required=True,
        help="Timestamp base de las corridas (sin sufijo C)",
    )
    parser.add_argument("--data-dir", default="data", help="Directorio de datos")
    parser.add_argument(
        "--scenarios",
        nargs="+",
        type=int,
        default=[0, 1, 2],
        help="Escenarios a comparar (default: 0 1 2)",
    )
    args = parser.parse_args()

    plt.figure(figsize=(9, 6))

    for sc in args.scenarios:
        file_path = Path(args.data_dir) / f"{args.timestamp}C-scenario{sc}.txt"
        if not file_path.exists():
            print(f"Advertencia: No se encontró {file_path}, saltando escenario {sc}")
            continue

        data = np.loadtxt(file_path)
        if data.ndim == 1:
            data = data.reshape(1, -1)

        eta = data[:, 0]
        order = data[:, 1]
        error = data[:, 2] if data.shape[1] >= 3 else None

        label = SCENARIO_LABELS.get(sc, f"Escenario {sc}")
        color = SCENARIO_COLORS.get(sc, None)
        marker = SCENARIO_MARKERS.get(sc, "o")

        if error is not None:
            plt.errorbar(
                eta,
                order,
                yerr=error,
                fmt=f"{marker}-",
                color=color,
                elinewidth=1,
                capsize=3,
                linewidth=1.5,
                label=label,
            )
        else:
            plt.plot(
                eta,
                order,
                marker=marker,
                linestyle="-",
                color=color,
                linewidth=1.5,
                label=label,
            )

    plt.xlabel(r"$\eta$", fontsize=13)
    plt.ylabel(r"$v_a$", fontsize=13)
    plt.title(r"Polarización $v_a$ vs ruido $\eta$ — Comparación de escenarios")
    plt.ylim(-0.05, 1.05)
    plt.grid(True, linestyle="--", alpha=0.7)
    plt.legend(fontsize=11)
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()
