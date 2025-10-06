import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from tqdm import tqdm
from pathlib import Path
import time
from energy_analysis import calculate_energies, parse_simulation_output


def energy_stddev_for_file(filename: Path) -> float | None:
    """
    Calcula la desviación estándar de la energía total para un archivo de simulación.
    Muestra el progreso de cálculo por cada timestep.
    """
    if not filename.is_file():
        tqdm.write(f"⚠️  Archivo no encontrado: {filename.name}")
        return None

    # --- Parse CSV ---
    t0 = time.time()
    data_by_time = parse_simulation_output(filename)
    if not data_by_time or not isinstance(data_by_time, dict):
        tqdm.write(f"⚠️  Datos inválidos en {filename.name}")
        return None
    parse_time = time.time() - t0

    # --- Compute energies ---
    times = sorted(data_by_time)
    total_energies = []

    for t in tqdm(
            times,
            desc=f"  → {filename.name:15} | Energías",
            leave=False,
            ncols=100,
    ):
        particles = data_by_time[t]
        _, _, te = calculate_energies(particles)
        total_energies.append(te)

    stddev = float(np.std(total_energies))
    total_time = time.time() - t0

    tqdm.write(f"✅ {filename.name:20} | σ(E)={stddev:.6g} | t={total_time:.2f}s (parse={parse_time:.2f}s)")
    return stddev


def main():
    """Analiza múltiples archivos de simulación generados con diferentes timeSteps."""
    timesteps_to_analyze = [0.1, 0.01, 0.001, 0.0001]
    data_directory = Path(__file__).parent / "data"
    results = {}

    print("🔍 Analizando la estabilidad de la energía...\n")

    for dt in tqdm(
            timesteps_to_analyze,
            desc="Procesando archivos",
            ncols=100,
            colour="cyan",
    ):
        filename = data_directory / f"sim_dt_{dt}.csv"
        std_dev = energy_stddev_for_file(filename)
        if std_dev is not None:
            results[dt] = std_dev

    if not results:
        print("\n⚠️ No se pudo analizar ningún archivo. "
              "Verifica que existan CSVs en la carpeta 'data'.")
        return

    # --- Plot ---
    plt.style.use('seaborn-v0_8-whitegrid')
    fig, ax = plt.subplots(figsize=(10, 6))

    sorted_items = sorted(results.items(), reverse=True)
    dts = [str(k) for k, _ in sorted_items]  # treat dt as labels
    stds = [v for _, v in sorted_items]

    ax.plot(dts, stds, marker='o', linestyle='-', color='b')

    ax.invert_xaxis()
    ax.set_xlabel('Paso de Tiempo (dt) [s]')
    ax.set_ylabel('Desviación Estándar de la Energía Total')
    ax.grid(True, which="both", ls="--")
    ax.minorticks_on()

    # --- Summary ---
    print("\n--- Resumen ---")
    for dt, std in sorted(results.items(), reverse=True):
        print(f"dt = {dt:<10} → σ(E) = {std:.6g}")

    plt.tight_layout()
    output_filename = Path(__file__).parent / "optimal_time_analysis.png"
    plt.savefig(output_filename, dpi=150)
    print(f"\n📊 Gráfico guardado en: {output_filename}")

    plt.show()


if __name__ == "__main__":
    main()
