#!/usr/bin/env python3
import argparse
import os
import re
from pathlib import Path
from typing import Dict, List, Optional, Tuple


import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation, PillowWriter
import numpy as np


def parse_static_file(path: Path) -> Tuple[int, float, str, Optional[int]]:
    with path.open("r", encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]
    n = int(float(lines[0]))
    l = float(lines[1])
    scenario = "STANDARD"
    leader_id: Optional[int] = None
    # Optional metadata line: SCENARIO <name> LEADER_ID <id>
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
    return n, l, scenario, leader_id


def parse_dynamic_file(path: Path, n_expected: int) -> List[np.ndarray]:
    """Parse dynamic file into a list of frames (x, y for each particle)."""
    with path.open("r", encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]

    frames: List[np.ndarray] = []
    i = 0
    total = len(lines)

    while i < total:
        parts = lines[i].split()
        # If this line is a single value, treat it as a time header and skip it
        if len(parts) == 1:
            i += 1
        if i + n_expected > total:
            break

        frame_xy = []
        for _ in range(n_expected):
            vals = lines[i].split()
            if len(vals) < 2:
                raise ValueError(f"Expected x y at line {i}, got: {lines[i]}")
            frame_xy.append((float(vals[0]), float(vals[1])))
            i += 1
        frames.append(np.array(frame_xy, dtype=float))

    if not frames:
        raise ValueError(f"No frames parsed from {path}")

    return frames


def parse_neighbors_output(path: Path) -> Tuple[List[Dict[int, List[int]]], List[int]]:
    """
    Expected blocks:
      Iteration: k
      [1 3 8]
      [2 5]
      ...
    """
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
    n, l, scenario, leader_id = parse_static_file(static_path)
    dynamic_frames = parse_dynamic_file(dynamic_path, n)
    frames_data, frame_numbers = parse_neighbors_output(neighbors_path)

    def xy_for_frame(idx: int) -> np.ndarray:
        return dynamic_frames[min(idx, len(dynamic_frames) - 1)]

    fig, ax = plt.subplots(figsize=(6, 6))
    ax.set_xlim(0, l)
    ax.set_ylim(0, l)
    ax.set_aspect("equal", adjustable="box")
    ax.set_title("Vicsek/CIM Neighbors Animation")
    ax.set_xlabel("x")
    ax.set_ylabel("y")

    first_counts = np.array([len(frames_data[0].get(i + 1, [])) for i in range(n)], dtype=float)
    xy0 = xy_for_frame(0)

    scat = ax.scatter(
        xy0[:, 0],
        xy0[:, 1],
        c=first_counts,
        cmap="viridis",
        s=25,
        vmin=0,
        vmax=max(1, first_counts.max()),
    )
    cbar = fig.colorbar(scat, ax=ax, fraction=0.046, pad=0.04)
    cbar.set_label("Neighbor count")

    text = ax.text(
        0.02, 0.98, "", transform=ax.transAxes, ha="left", va="top",
        bbox=dict(facecolor="white", alpha=0.7, edgecolor="none")
    )

    leader_mark = None
    highlight_leader = leader_id is not None and scenario in {"LEADER", "CIRCULAR_LEADER"}
    if highlight_leader:
        idx = leader_id - 1
        leader_mark = ax.scatter(
            xy0[idx, 0],
            xy0[idx, 1],
            marker="*",
            s=120,
            c="red",
            edgecolors="black",
            linewidths=0.8,
            zorder=5,
            label="Leader",
        )
        ax.legend(loc="upper right")

    def update(frame_idx):
        frame = frames_data[frame_idx]
        counts = np.array([len(frame.get(i + 1, [])) for i in range(n)], dtype=float)
        xy = xy_for_frame(frame_idx)

        scat.set_offsets(xy)
        scat.set_array(counts)
        scat.set_clim(0, max(1, counts.max()))
        text.set_text(f"Iteration: {frame_numbers[frame_idx]}")

        if highlight_leader and leader_mark is not None:
            idx = leader_id - 1
            leader_mark.set_offsets(xy[idx])

        return scat, text, leader_mark if leader_mark is not None else scat

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
        print(f"Saved animation to: {save_path}")
        plt.close(fig)
        return

    plt.tight_layout()
    plt.show()


def main():
    parser = argparse.ArgumentParser(description="Animate SDS neighbor iterations in an LxL box.")
    parser.add_argument("--timestamp", required=True, help="Timestamp prefix (e.g. 1773437098)")
    parser.add_argument("--data-dir", default="data", help="Directory containing the files (default: data)")
    parser.add_argument("--interval", type=int, default=120, help="Frame interval in ms (default: 120)")
    parser.add_argument("--save", default=None, help="Optional output animation path (.gif or .mp4)")
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
