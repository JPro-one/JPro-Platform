# Markdown Example File

## Table of Contents
1. [Emphasis](#emphasis)
2. [Lists](#lists)
3. [Links](#links)
4. [Code Blocks](#code-blocks)
5. [Tables](#tables)
6. [Block Quotes](#block-quotes)

---

## Emphasis

*This text is italicized.* **This text is bold.** *You **can** combine them.* ~~And strikethrough too.~~

---

## Lists

### Task List

- [x] Implement syntax highlighting
- [x] Add user agent stylesheets
- [ ] World domination

### Ordered List

1. First item
2. Second item
   1. Sub-item A
   2. Sub-item B
3. Third item

---

## Links

[Visit JPro](https://www.jpro.one) — [View on GitHub](https://github.com/JPro-one/JPro-Platform)

---

## Code Blocks

### Java

```java
public class QuickSort<T extends Comparable<T>> {

    public void sort(T[] array, int low, int high) {
        if (low < high) {
            int pivot = partition(array, low, high);
            sort(array, low, pivot - 1);
            sort(array, pivot + 1, high);
        }
    }

    private int partition(T[] array, int low, int high) {
        T pivot = array[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (array[j].compareTo(pivot) <= 0) {
                i++;
                T temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        T temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;
        return i + 1;
    }
}
```

### Python

```python
from dataclasses import dataclass
from typing import Optional

@dataclass
class TreeNode:
    value: int
    left: Optional["TreeNode"] = None
    right: Optional["TreeNode"] = None

def inorder(node: Optional[TreeNode]) -> list[int]:
    if node is None:
        return []
    return inorder(node.left) + [node.value] + inorder(node.right)

# Build a small tree
root = TreeNode(4,
    left=TreeNode(2, TreeNode(1), TreeNode(3)),
    right=TreeNode(6, TreeNode(5), TreeNode(7))
)
print(inorder(root))  # [1, 2, 3, 4, 5, 6, 7]
```

### JavaScript

```javascript
async function fetchUsers(endpoint) {
  try {
    const response = await fetch(endpoint);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const users = await response.json();
    return users.filter(u => u.active).map(({ name, email }) => ({ name, email }));
  } catch (err) {
    console.error("Failed to fetch users:", err.message);
    return [];
  }
}

// Debounce utility
const debounce = (fn, ms) => {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
};
```

### SQL

```sql
SELECT
    d.name          AS department,
    COUNT(e.id)     AS headcount,
    ROUND(AVG(e.salary), 2) AS avg_salary
FROM employees e
JOIN departments d ON d.id = e.department_id
WHERE e.hire_date >= '2023-01-01'
GROUP BY d.name
HAVING COUNT(e.id) > 5
ORDER BY avg_salary DESC;
```

### Bash

```bash
#!/usr/bin/env bash
set -euo pipefail

LOG_FILE="/var/log/deploy-$(date +%Y%m%d-%H%M%S).log"

deploy() {
    local env="${1:?Usage: deploy <env>}"
    echo "Deploying to $env ..." | tee -a "$LOG_FILE"

    git pull origin main 2>&1 | tee -a "$LOG_FILE"
    ./gradlew clean build -x test 2>&1 | tee -a "$LOG_FILE"

    if [[ "$env" == "production" ]]; then
        read -rp "Are you sure? (y/N) " confirm
        [[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 1; }
    fi

    echo "Deploy complete."
}

deploy "$@"
```

### CSS

```css
:root {
    --primary: #2563eb;
    --radius: 8px;
}

.card {
    border-radius: var(--radius);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

@media (prefers-color-scheme: dark) {
    .card {
        background: #1e293b;
        color: #f1f5f9;
    }
}
```

### Plain code (no language)

```
This is a plain code block with no syntax highlighting.
It preserves    spacing   and
  indentation as-is.
```

---

## Tables

| Language   | Typing     | Use Case               |
|------------|------------|------------------------|
| Java       | Static     | Enterprise, Android    |
| Python     | Dynamic    | ML, Scripting          |
| Rust       | Static     | Systems, WebAssembly   |
| JavaScript | Dynamic    | Web, Full-stack        |

---

## Block Quotes

> Any sufficiently advanced technology is indistinguishable from magic.
>
> — Arthur C. Clarke

---

## Images and extensions

Some Image:
![alt](https://www.jpro.one/app/default/resourcesencoded/cp:/1/1/one/jpro/img/landing/DUKE-forward.png)
