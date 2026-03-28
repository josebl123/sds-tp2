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

    plt.xlabel(r"$\eta$", fontsize=16)
    plt.ylabel(r"$v_a$", fontsize=16)
    plt.ylim(-0.05, 1.05)
    plt.grid(True, linestyle="--", alpha=0.7)
    plt.legend(fontsize=11)
    output_dir = Path(args.data_dir) / "plots"
    output_dir.mkdir(parents=True, exist_ok=True)
    scenarios_str = "".join(map(str, sorted(args.scenarios)))
    output_path = output_dir / f"comparison_va_eta_{args.timestamp}C_sc{scenarios_str}.png"

    plt.tight_layout()
    plt.savefig(output_path, dpi=300)
    print(f"Grafico guardado en {output_path}")


if __name__ == "__main__":
    main()
