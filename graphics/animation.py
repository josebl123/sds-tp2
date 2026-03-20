import argparse
import os
import re
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation, PillowWriter
import numpy as np

def parse_static_file(path: Path) -> Tuple[int, float, str, Optional[int], Optional[Tuple[float, float]], Optional[float]]:
    with path.open("r", encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]
    n = int(float(lines[0]))
    l = float(lines[1])
    scenario = "STANDARD"
    leader_id: Optional[int] = None
    circle_center: Optional[Tuple[float, float]] = None
    circle_radius: Optional[float] = None

    if len(lines) > 2 and lines[2].startswith("SCENARIO"):
        parts = lines[2].split()
        try:
            scenario_idx = parts.index("SCENARIO")
            scenario = parts[scenario_idx + 1]
        except Exception:
            scenario = "STANDARD"
        try:
            leader_idx = parts.index("LEADER_ID")
            leader_id = int(parts[leader_idx + 1])
        except Exception:
            leader_id = None

        if scenario == "CIRCULAR_LEADER":
            try:
                center_idx = parts.index("CIRCLE_CENTER")
                circle_center = (float(parts[center_idx + 1]), float(parts[center_idx + 2]))
            except Exception:
                circle_center = None
            try:
                radius_idx = parts.index("CIRCLE_RADIUS")
                circle_radius = float(parts[radius_idx + 1])
            except Exception:
                circle_radius = None

    return n, l, scenario, leader_id, circle_center, circle_radius

def parse_dynamic_file(path: Path, n_expected: int) -> List[np.ndarray]:
    with path.open("r", encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]

    frames: List[np.ndarray] = []
    i = 0
    total = len(lines)

    while i < total:
        parts = lines[i].split()
        if len(parts) == 1:
            i += 1
        if i + n_expected > total:
            break

        frame_data = []
        for _ in range(n_expected):
            vals = lines[i].split()
            if len(vals) < 4:
                raise ValueError(f"Expected x y vx vy at line {i}, got: {lines[i]}")
            frame_data.append((float(vals[0]), float(vals[1]), float(vals[2]), float(vals[3])))
            i += 1
        frames.append(np.array(frame_data, dtype=float))

    if not frames:
        raise ValueError(f"No frames parsed from {path}")

    return frames

def parse_neighbors_output(path: Path) -> Tuple[List[Dict[int, List[int]]], List[int]]:
    frames: List[Dict[int, List[int]]] = []
    frame_numbers: List[int] = []

    it_pattern = re.compile(r"^Iteration:\s*(\d+)\s*$")
    line_pattern = re.compile(r"^\[(.*)\]$")

    current: Optional[Dict[int, List[int]]] = None
    particle_idx = 1

    with path.open("r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line:
                continue

            m_it = it_pattern.match(line)
            if m_it:
                if current is not None:
                    frames.append(current)
                current = {}
                frame_numbers.append(int(m_it.group(1)))
                particle_idx = 1
                continue

            m_line = line_pattern.match(line)
            if m_line and current is not None:
                content = m_line.group(1).strip()
                neigh = [int(t) for t in content.split()] if content else []
                current[particle_idx] = neigh
                particle_idx += 1

    if current is not None:
        frames.append(current)

    if not frames:
        raise ValueError(f"No iteration data found in {path}")

    return frames, frame_numbers

def animate(static_path: Path, dynamic_path: Path, neighbors_path: Path, interval_ms: int, save_path: Optional[Path]):
    n, l, scenario, leader_id, circle_center, circle_radius = parse_static_file(static_path)
    dynamic_frames = parse_dynamic_file(dynamic_path, n)
    frames_data, frame_numbers = parse_neighbors_output(neighbors_path)

    def data_for_frame(idx: int) -> np.ndarray:
        return dynamic_frames[min(idx, len(dynamic_frames) - 1)]

    fig, ax = plt.subplots(figsize=(8, 8))
    ax.set_xlim(0, l)
    ax.set_ylim(0, l)
    ax.set_aspect("equal", adjustable="box")
    ax.set_title("Animacion de Bandadas")
    ax.set_xlabel("x")
    ax.set_ylabel("y")

    data0 = data_for_frame(0)
    x0, y0, u0, v0 = data0[:, 0], data0[:, 1], data0[:, 2], data0[:, 3]
    angles0 = np.arctan2(v0, u0)
    if scenario == "CIRCULAR_LEADER" and circle_center and circle_radius:
        # Dibuja el círculo y sus imágenes periódicas
        for dx in [-l, 0, l]:
            for dy in [-l, 0, l]:
                center = (circle_center[0] + dx, circle_center[1] + dy)
                trayectoria = plt.Circle(
                    center,
                    circle_radius,
                    color='gray',
                    fill=False,
                    linestyle='--',
                    zorder=1,
                    alpha=0.5
                )
                ax.add_patch(trayectoria)
    quiv = ax.quiver(
        x0, y0, u0, v0, angles0,
        cmap="hsv",
        pivot="mid",
        scale=1.5,
        width=0.004,
        clim=(-np.pi, np.pi)
    )

    cbar = fig.colorbar(quiv, ax=ax, fraction=0.046, pad=0.04)
    cbar.set_label("Angulo de Velocidad (rad)")
    cbar.set_ticks([-np.pi, -np.pi / 2, 0, np.pi / 2, np.pi])
    cbar.set_ticklabels(["-π", "-π/2", "0", "π/2", "π"])

    text = ax.text(
        0.02, 0.98, "", transform=ax.transAxes, ha="left", va="top",
        bbox=dict(facecolor="white", alpha=0.7, edgecolor="none")
    )

    # Destacar al líder sin tapar su vector
    leader_mark = None
    highlight_leader = leader_id is not None and scenario in {"LEADER", "CIRCULAR_LEADER"}
    if highlight_leader:
        idx = leader_id - 1
        leader_mark = ax.scatter(
            x0[idx],
            y0[idx],
            marker="o",          # Círculo en lugar de estrella
            s=300,               # Tamaño lo suficientemente grande para rodear el vector
            facecolors="none",   # Transparente por dentro para que se vea el vector
            edgecolors="black",  # Borde negro bien definido
            linewidths=2.0,
            zorder=5,
            label="Lider"
        )
        ax.legend(loc="upper right")

    # Etiquetas con el id de cada partícula
    # id_labels = [
    #     ax.text(
    #         x0[i],
    #         y0[i],
    #         str(i + 1),
    #         ha="center",
    #         va="center",
    #         fontsize=7,
    #         color="black",
    #         zorder=6,
    #         bbox=dict(facecolor="white", alpha=0.6, edgecolor="none"),
    #     )
    #     for i in range(len(x0))
    # ]

    def update(frame_idx):
        data = data_for_frame(frame_idx)
        x, y, u, v = data[:, 0], data[:, 1], data[:, 2], data[:, 3]
        angles = np.arctan2(v, u)

        quiv.set_offsets(np.c_[x, y])
        quiv.set_UVC(u, v, angles)

        text.set_text(f"Iteracion: {frame_numbers[frame_idx]}")

        if highlight_leader and leader_mark is not None:
            idx = leader_id - 1
            leader_mark.set_offsets(np.c_[x[idx], y[idx]])

        # for i, label in enumerate(id_labels):
        #     label.set_position((x[i], y[i]))

        return quiv, text, leader_mark if leader_mark is not None else quiv

    anim = FuncAnimation(
        fig,
        update,
        frames=len(frames_data),
        interval=interval_ms,
        blit=False,
        repeat=True,
    )

    if save_path is not None:
        suffix = save_path.suffix.lower()
        if suffix == ".gif":
            fps = 1000 / max(interval_ms, 1)
            anim.save(save_path.as_posix(), writer=PillowWriter(fps=fps))
        elif suffix in {".mp4", ".m4v"}:
            anim.save(save_path.as_posix(), writer="ffmpeg")
        else:
            anim.save(save_path.as_posix())
        print(f"Animacion guardada en: {save_path}")
        plt.close(fig)
        return

    plt.tight_layout()
    plt.show()

def main():
    parser = argparse.ArgumentParser(description="Animacion SdS Autómatas")
    parser.add_argument("--timestamp", required=True)
    parser.add_argument("--data-dir", default="data")
    parser.add_argument("--interval", type=int, default=50)
    parser.add_argument("--save", default=None)
    args = parser.parse_args()

    base = Path(args.data_dir) / args.timestamp
    static_path = base.with_suffix(".txt")
    dynamic_path = Path(f"{base}-Dynamic.txt")
    neighbors_path = Path(f"{base}-output.txt")

    animate(
        static_path=static_path,
        dynamic_path=dynamic_path,
        neighbors_path=neighbors_path,
        interval_ms=args.interval,
        save_path=Path(args.save) if args.save else None,
    )

if __name__ == "__main__":
    main()