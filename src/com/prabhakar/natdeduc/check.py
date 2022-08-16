file = open("output.txt")
props = []
i = 0

for line in file:
	if line in props:
		i += 1

	props.append(line)

print(f"{i} repeats.")