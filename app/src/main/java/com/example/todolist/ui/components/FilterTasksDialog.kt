package com.example.todolist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTasksDialog(
    allAvailableCategories: List<String>,
    initiallySelectedCategories: Set<String>,
    initiallyShowOnlyHidden: Boolean,
    onDismissRequest: () -> Unit,
    onApplyFilters: (selectedCategories: Set<String>, showOnlyHidden: Boolean) -> Unit
) {
    var tempSelectedCategoriesState by remember { mutableStateOf(initiallySelectedCategories.toSet()) }
    var tempShowOnlyHidden by remember { mutableStateOf(initiallyShowOnlyHidden) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(0.95f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Filtruj Zadania", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tempShowOnlyHidden = !tempShowOnlyHidden }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = tempShowOnlyHidden,
                        onCheckedChange = { tempShowOnlyHidden = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pokaż tylko ukryte zadania")
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Wybierz kategorie:", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Jeśli nie zaznaczysz żadnej, zostaną pokazane zadania ze wszystkich kategorii (zgodnie z filtrem ukrytych).",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (allAvailableCategories.isEmpty()) {
                    Text("Brak dostępnych kategorii.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(allAvailableCategories) { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentSelection = tempSelectedCategoriesState.toMutableSet()
                                        if (currentSelection.contains(category)) {
                                            currentSelection.remove(category)
                                        } else {
                                            currentSelection.add(category)
                                        }
                                        tempSelectedCategoriesState = currentSelection.toSet()
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = tempSelectedCategoriesState.contains(category),
                                    onCheckedChange = { isChecked ->
                                        val currentSelection = tempSelectedCategoriesState.toMutableSet()
                                        if (isChecked) {
                                            currentSelection.add(category)
                                        } else {
                                            currentSelection.remove(category)
                                        }
                                        tempSelectedCategoriesState = currentSelection.toSet()
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(category)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onApplyFilters(tempSelectedCategoriesState, tempShowOnlyHidden)
                    }) {
                        Text("Zastosuj")
                    }
                }
            }
        }
    }
}
