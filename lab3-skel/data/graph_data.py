import pandas as pd
import matplotlib.pyplot as plt
import sys

def main():
    csv_file_path = sys.argv[1]
    output_name = sys.argv[2]
    df = pd.read_csv(csv_file_path)

    df['average_time'] = df['average_time'] / 1_000_000_000
    
    plt.figure(figsize=(12, 8))
    
    # Create combinations of distribution and mix for grouping
    for (distribution, mix), group in df.groupby(['distribution', 'mix']):
        group = group.sort_values('threads')
        
        # Create label based on distribution and mix
        if distribution == "Uniform" and mix == '1:1:8':
            label = 'A1'
        elif distribution == "Uniform" and mix == '1:1:0':
            label = 'A2'
        elif distribution == "Normal" and mix == '1:1:8':
            label = 'B1'
        else:
            label = 'B2'
        
        plt.plot(
            group['threads'],
            group['average_time'],
            label=label,
            marker='o',
            linestyle='-',
            linewidth=2,
            markersize=6
        )
    
    plt.xlabel('Number of Threads', fontsize=12)
    plt.ylabel('Average Execution Time (s)', fontsize=12)
    plt.title('Average Execution Time vs Number of Threads\nGrouped by Distribution and Mix', fontsize=14)
    plt.legend(title='Distribution & Mix', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.grid(True, alpha=0.3)
    
    # Adjust layout to prevent legend cutoff
    plt.tight_layout()

    plt.savefig(output_name, dpi=300, bbox_inches='tight')

if __name__ == "__main__":
    main()